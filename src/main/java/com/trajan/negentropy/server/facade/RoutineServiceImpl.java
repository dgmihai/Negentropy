package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineEntityHierarchy;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import com.trajan.negentropy.model.entity.QTaskLink;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.*;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.entity.sync.SyncRecordEntity;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.SerializationUtil;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordDataType;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.backend.repository.RoutineStepRepository;
import com.trajan.negentropy.server.backend.repository.SyncRecordRepository;
import com.trajan.negentropy.server.backend.sync.SyncManager;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.RoutineUtil;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Transactional
public class RoutineServiceImpl implements RoutineService {
    private static final Logger logger = LoggerFactory.getLogger(RoutineServiceImpl.class);

    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationService netDurationService;
    @Autowired private ChangeService changeService;

    @Autowired private SyncManager syncManager;

    @Autowired private SyncRecordRepository syncRecordRepository;
    @Autowired private LinkRepository linkRepository;

    @Autowired private DataContext dataContext;

    private static final Set<TimeableStatus> FINALIZED_STATUSES = Set.of(
            TimeableStatus.COMPLETED,
            TimeableStatus.EXCLUDED,
            TimeableStatus.POSTPONED);

    @PostConstruct
    public void onStart() {
        for (RoutineEntity routine : routineRepository.findAll()) {
            if (routine.status().equals(TimeableStatus.COMPLETED) ||
                routine.status().equals(TimeableStatus.SKIPPED)) {
                this.cleanUpRoutine(routine);
            }
        }
    }

    @Override
    public Routine fetchRoutine(RoutineID routineID) {
        logger.trace("fetchRoutine");
        return dataContext.toDO(entityQueryService.getRoutine(routineID));
    }

    @Override
    public RoutineStep fetchRoutineStep(StepID stepID) {
        logger.trace("fetchRoutineStep");
        return dataContext.toDO(entityQueryService.getRoutineStep(stepID));
    }

    private RoutineResponse process(Supplier<RoutineEntity> routineSupplier) {
        try {
            RoutineEntity routine = routineSupplier.get();
            Routine routineDO = dataContext.toDO(routine);
            return new RoutineResponse(true, routineDO, K.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new RoutineResponse(false, null, e.getMessage());
        }
    }

    private RoutineResponse process(LocalDateTime time, Supplier<RoutineEntity> routineSupplier) {
        try {
            RoutineEntity routine = routineSupplier.get();
            RoutineUtil.setRoutineDuration(routine, time);
            Routine routineDO = dataContext.toDO(routine);
            return new RoutineResponse(true, routineDO, K.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new RoutineResponse(false, null, e.getMessage());
        }
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId) {
        return this.createRoutine(rootId, null);
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId) {
        return this.createRoutine(rootId, null);
    }

    // Common function
    private RoutineEntity createRoutineInternal(Data root, TaskNodeTreeFilter filter) {
        Duration durationLimit = filter != null ? filter.durationLimit() : null;
        NonSpecificTaskNodeTreeFilter processedFilter = NonSpecificTaskNodeTreeFilter.from(filter);
        logger.trace("Populate routine with filter: " + processedFilter);

        RoutineEntity routine = new RoutineEntity();

        try {
            routine.serializedFilter(SerializationUtil.serialize(NonSpecificTaskNodeTreeFilter.from(filter)));
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize filter: " + processedFilter, e);
        }

        RoutineEntityHierarchy hierarchy = new RoutineEntityHierarchy(routine);

        NetDurationHelper helper = netDurationService.getHelper(processedFilter);

        if (root instanceof TaskEntity) {
            routine.estimatedDuration(helper.getNetDuration((TaskEntity) root, hierarchy, durationLimit));
        } else if (root instanceof TaskLink) {
            routine.estimatedDuration(helper.getNetDuration((TaskLink) root, hierarchy, durationLimit));
        } else {
            throw new IllegalArgumentException("Root must be either a TaskEntity or a TaskLink.");
        }

        RoutineEntity result = routineRepository.save(routine);
        logger.debug("Created routine: " + result.currentStep().task().name() + " with " + result.countSteps() + " steps.");
        return result;
    }

    @Override
    public RoutineResponse recalculateRoutine(@NotNull RoutineID routineId, LocalDateTime time) {
        return this.process(() -> {
            RoutineEntity routine = entityQueryService.getRoutine(routineId);

            SyncRecord syncRecord = syncManager.aggregatedSyncRecord(routine.syncId());

            if (syncRecord.id().equals(routine.syncId())) {
                return routine;
            }

            SyncRecordEntity syncRecordEntity = syncRecordRepository.getReferenceById(syncRecord.id().val());

            RoutineStepEntity currentStep = routine.currentStep();

            Set<Long> persistedLinks = new HashSet<>();
            Map<Long, Duration> mergedLinksWithPreviousDuration = new HashMap<>();

            syncRecordEntity.changes().forEach(change -> {
                if (change.dataType().equals(ChangeRecordDataType.LINK)) {
                    switch (change.changeType()) {
                        case MERGE -> mergedLinksWithPreviousDuration.put(change.entityId(), change.previousDuration());
                        case PERSIST -> {
                            persistedLinks.add(change.entityId());
                            mergedLinksWithPreviousDuration.remove(change.entityId());
                        }
                        case DELETE -> {
                            persistedLinks.remove(change.entityId());
                            mergedLinksWithPreviousDuration.remove(change.entityId());
                        }
                    }
                }
            });

            try {
                TaskNodeTreeFilter routineFilter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());
                NetDurationHelper helper = netDurationService.getHelper(routineFilter);

                BooleanBuilder mergedLinksPredicate = new BooleanBuilder(
                        QRoutineStepEntity.routineStepEntity.link.id.in(mergedLinksWithPreviousDuration.keySet()))
                        .and(QRoutineStepEntity.routineStepEntity.routine.eq(routine));
                // TODO: Check if existing tasks match filter
                routineStepRepository.findAll(mergedLinksPredicate).forEach(step -> {
                    logger.debug("Updating duration of step " + step.task().name() + " in routine " + routine.id() + ".");
                    Duration difference = step.duration()
                            .minus(mergedLinksWithPreviousDuration.get(step.link().get().id()));
                    routine.estimatedDuration(routine.estimatedDuration().plus(difference));
                });

                BooleanBuilder deletedLinksPredicate = new BooleanBuilder(
                        QRoutineStepEntity.routineStepEntity.deletedLink
                        .and(QRoutineStepEntity.routineStepEntity.routine.eq(routine)));
                routineStepRepository.findAll(deletedLinksPredicate).forEach(step -> {
                    step.parentStep().children().remove(step);
                    step.routine(null);
                    routine.estimatedDuration(routine.estimatedDuration().minus(step.duration()));
                    for (int i = step.position(); i < step.parentStep().children().size(); i++) {
                        step.parentStep().children().get(i).position(i);
                    }
                    routineStepRepository.delete(step);
                });

                BooleanBuilder persistedLinksPredicate = new BooleanBuilder(
                        QTaskLink.taskLink.id.isNotNull())
                        .and(QTaskLink.taskLink.id.in(persistedLinks))
                        .and(entityQueryService.filterLinkPredicate(routineFilter));

                Set<Long> persistedParentTaskIds = new HashSet<>();
                MultiValueMap<Long, TaskLink> persistedParentMap = new LinkedMultiValueMap<>();
                Iterable<TaskLink> persistedLinksFiltered = linkRepository.findAll(persistedLinksPredicate);

                persistedLinksFiltered.forEach(link -> {
                    if (link.parent() != null) {
                        persistedParentTaskIds.add(link.parent().id());
                        persistedParentMap.add(link.parent().id(), link);
                    }
                });

                BooleanBuilder parentStepsWithNewChildrenPredicate = new BooleanBuilder(
                        QRoutineStepEntity.routineStepEntity.routine.eq(routine))
                        .and(QRoutineStepEntity.routineStepEntity.task.id.in(persistedParentTaskIds));

                Iterable<RoutineStepEntity> parentStepsWithNewChildren = routineStepRepository.findAll(
                        parentStepsWithNewChildrenPredicate);

                parentStepsWithNewChildren.forEach(parentStep -> {
                    List<TaskLink> newChildren = persistedParentMap.get(parentStep.task().id());
                    newChildren.sort(Comparator.comparing(TaskLink::position));
                    for (int i = 0; i < newChildren.size(); i++) {
                        RoutineStepEntity child = parentStep.children().get(i);
                        if (newChildren.get(i).position() > child.position()) {
                            logger.debug("Adding new child " + newChildren.get(i).child().name() + " to parent " +
                                    parentStep.task().name() + " at position " + i);
                            RoutineStepEntityHierarchy parentHierarchy = new RoutineStepEntityHierarchy(parentStep);
                            parentHierarchy.customIndexOfChild(i);
                            Duration additionalDuration = helper.getNetDuration(newChildren.get(i), parentHierarchy, null);
                            logger.debug("Previous estimated duration: " + routine.estimatedDuration() + ", additional duration: " + additionalDuration);
                            routine.estimatedDuration(routine.estimatedDuration().plus(additionalDuration));
                            logger.debug("New estimated duration: " + routine.estimatedDuration());
                        }
                    }
                });

                routine.currentPosition(routine.getAllChildren().indexOf(currentStep));
                if (routine.currentPosition() == -1) {
                    routine.currentPosition(0);
                }

                return routineRepository.save(routine
                        .syncId(syncRecord.id().val())
                        .estimatedDuration(RoutineUtil.getRemainingRoutineDuration(routine, time)));
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Could not deserialize filter: " + routine.serializedFilter(), e);
                throw new RuntimeException("Could not deserialize filter: " + routine.serializedFilter(), e);
            }
        });
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, TaskNodeTreeFilter filter) {
        return this.process(() -> {
            TaskEntity rootTask = entityQueryService.getTask(rootId);
            return createRoutineInternal(rootTask, filter);
        });
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId, TaskNodeTreeFilter filter) {
        return this.process(() -> {
            TaskLink rootLink = entityQueryService.getLink(rootId);
            return createRoutineInternal(rootLink, filter);
        });
    }

    private Predicate filterByStatus(Set<TimeableStatus> statusSet) {
        logger.trace("filterByStatus");
        QRoutineEntity qRoutine = QRoutineEntity.routineEntity;
        BooleanBuilder builder = new BooleanBuilder();
        statusSet.forEach(status -> builder.or(qRoutine.status.eq(status)));

        return builder;
    }

    @Override
    public long countCurrentRoutines(Set<TimeableStatus> statusSet) {
        logger.trace("countCurrentRoutines");
        return routineRepository.count(
                this.filterByStatus(statusSet));
    }

    @Override
    public Stream<Routine> fetchRoutines(Set<TimeableStatus> statusSet) {
        logger.trace("fetchRoutines");
        return StreamSupport.stream(
                routineRepository.findAll(this.filterByStatus(statusSet)).spliterator(), false)
                .map(dataContext::toDO);
    }

    private void cleanUpRoutine(RoutineEntity routine) {
        for (RoutineStepEntity step : routine.getAllChildren()) {
            if (step.status().equals(TimeableStatus.ACTIVE) || step.status().equals(TimeableStatus.NOT_STARTED)) {
                step.status(TimeableStatus.SKIPPED);
            }
        }
    }

    @Override
    public RoutineResponse startStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();

        return process(time, () -> activateStep(routine, time));
    }

    private RoutineEntity activateStep(RoutineEntity routine, LocalDateTime time) {
        RoutineStepEntity step = routine.currentStep();
        logger.debug("Activating step " + step.task().name() + " in routine " + routine.id() + ".");
        switch (step.status()) {
            case NOT_STARTED -> {
                step.startTime(time);
                step.lastSuspendedTime(time);
            }
            case SKIPPED, COMPLETED, EXCLUDED, POSTPONED -> {
                step.elapsedSuspendedDuration(
                        Duration.between(step.finishTime(), time)
                                .plus(step.elapsedSuspendedDuration()));
                step.finishTime(null);
            }
            case SUSPENDED -> {
                step.elapsedSuspendedDuration(
                        Duration.between(step.lastSuspendedTime(), time)
                                .plus(step.elapsedSuspendedDuration()));
                step.lastSuspendedTime(time);
            }
        }

        unCompleteStep(step, time);
        if (step.startTime() == null) {
            step.startTime(time);
        }

        step.status(TimeableStatus.ACTIVE);
        if (routine.status().equals(TimeableStatus.NOT_STARTED)) {
            routine.status(TimeableStatus.ACTIVE);
        }

        return routine;
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        return process(time, () -> completeStep(step, time));
    }

    private RoutineEntity completeStep(RoutineStepEntity step, LocalDateTime time) {
        logger.debug("Completing step " + step.task().name() + " in routine " + step.routine().id() + ".");

        switch (getStepStatusBasedOnChildrenStatus(step)) {
            case COMPLETED -> markStepAsCompleted(step, time);
            case POSTPONED -> markStepAsPostponed(step, time);
            case SKIPPED -> markStepAsSkipped(step, time);
            case EXCLUDED -> markStepAsExcluded(step, time);
            case ACTIVE -> activateStep(step.routine(), time);
        }

        return iterateToNextValidStep(step.routine(), 1, time);
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();

        return process(time, () -> suspendStep(step, time));
    }

    private RoutineEntity suspendStep(RoutineStepEntity step, LocalDateTime time) {
        logger.debug("Suspending step " + step.task().name() + " in routine " + step.routine().id());

        if (Objects.requireNonNull(step.status()) == TimeableStatus.ACTIVE) {
            step.lastSuspendedTime(time);
            step.status(TimeableStatus.SUSPENDED);
            unCompleteStep(step, time);
        }

        return step.routine();
    }

    @Override
    public RoutineResponse skipStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);

        return process(time, () -> skipStep(step, time));
    }

    private RoutineEntity skipStep(RoutineStepEntity step, LocalDateTime time) {
        logger.debug("Skipping step " + step.task().name() + " in routine " + step.routine().id() + ".");

        TimeableStatus skippedStatus = TimeableStatus.SKIPPED;

        int count = processChildren(step, time, skippedStatus);

        unCompleteStep(step, time);
        markStepAsSkipped(step, time);

        return iterateToNextValidStep(step.routine(), count, time);
    }

    @Override
    public RoutineResponse postponeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);

        return process(time, () -> postponeStep(step, time));
    }

    private RoutineEntity postponeStep(RoutineStepEntity step, LocalDateTime time) {
        logger.debug("Postponing step " + step.task().name() + " in routine " + step.routine().id() + ".");

        TimeableStatus postponedStatus = TimeableStatus.POSTPONED;

        int count = processChildren(step, time, postponedStatus);

        markStepAsPostponed(step, time);

        return iterateToNextValidStep(step.routine(), count, time);
    }

    @Override
    public RoutineResponse previousStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);

        return process(time, () -> previousStep(step, time));
    }

    private RoutineEntity previousStep(RoutineStepEntity step, LocalDateTime time) {
        logger.debug("Going to previous step of " + step.task().name() + " in routine " + step.routine().id());

        RoutineEntity routine = step.routine();

        if (currentPositionIsFirstStep(routine)) {
            throw new IllegalStateException("Cannot go to previous step when at the first step of a routine.");
        }

        suspendStep(step, time);
        if (routine.currentPosition() > 0) {
            routine.currentPosition(routine.currentPosition() - 1);
            while (routine.currentStep().status().equals(TimeableStatus.EXCLUDED)) {
                routine.currentPosition(routine.currentPosition() - 1);
            }
        }

        return routine;
    }

    @Override
    public RoutineResponse skipRoutine(RoutineID routineId, LocalDateTime time) {
        return process(time, () -> {
            RoutineEntity routine = entityQueryService.getRoutine(routineId);

            routine.status(TimeableStatus.SKIPPED);
            routine.currentStep().status(TimeableStatus.SKIPPED);
            routine.currentStep().finishTime(time);
            cleanUpRoutine(routine);

            return routine;
        });
    }

    private RoutineEntity completeRoutine(RoutineEntity routine) {
        logger.debug("Completing routine " + routine.id() + ".");
        routine.status(TimeableStatus.COMPLETED);
        cleanUpRoutine(routine);
        return routine;
    }

    @Override
    public RoutineResponse moveStep(StepID childId, StepID parentId, int position) {
        throw new NotImplementedException("Moving steps is not yet implemented.");
//        return process(() -> {
//            RoutineStepEntity step = entityQueryService.getRoutineStep(childId);
//            RoutineEntity routine = step.routine();
//
//            List<RoutineStepEntity> steps;
//            if (parentId == null) {
//                steps = step.routine().children();
//            } else {
//                RoutineStepEntity parentStep = entityQueryService.getRoutineStep(parentId);
//                if (!routine.equals(parentStep.routine())) {
//                    throw new RuntimeException(step + " and " + parentStep + " do not belong to the same routine.");
//                }
//                steps = parentStep.children();
//            }
//
//            int oldIndex = steps.indexOf(step);
//
//            steps.remove(step);
//            if (position > oldIndex) {
//                steps.add(position-1, step);
//            } else {
//                steps.add(position, step);
//            }
//
//            for (int i = 0; i < steps.size(); i++) {
//                steps.get(i).position(i);
//            }
//
//            return routine;
//        });
    }

    @Override
    public RoutineResponse setStepExcluded(StepID stepId, LocalDateTime time, boolean exclude) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Setting step " + step + " in routine " + routine.id() + " as excluded: " + exclude);

        return process(time, () -> setStepExcluded(step, time, exclude));
    }

    private RoutineEntity setStepExcluded(RoutineStepEntity step, LocalDateTime time, boolean exclude) {
        if (exclude) {
            suspendStep(step, time);
            markStepAsExcluded(step, time);
            unCompleteStep(step, time);
            if (step.routine().currentStep().equals(step)) {
                iterateToNextValidStep(step.routine(), 1, time);
            }
        } else {
            step.status(TimeableStatus.SKIPPED);
        }

        return step.routine();
    }

    // ================================
    // Boolean Helpers
    // ================================

    private boolean isStepFinalized(RoutineStepEntity step) {
        return FINALIZED_STATUSES.contains(step.status());
    }

    private boolean isRoutineFinished(RoutineEntity routine) {
        return (routine.currentPosition() >= routine.countSteps() - 1)
                && isStepFinalized(routine.currentStep());
    }

    private boolean isLastChild(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = step.parentStep() != null
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.indexOf(step) == siblings.size() - 1;
    }

    private TimeableStatus getStepStatusBasedOnChildrenStatus(RoutineStepEntity step) {
        int completedCount = 0;
        int excludedCount = 0;
        int postponedCount = 0;
        for (RoutineStepEntity child : step.children()) {
            switch (child.status()) {
                case COMPLETED -> completedCount++;
                case EXCLUDED -> excludedCount++;
                case POSTPONED -> postponedCount++;
            }
        }

        int size = step.children().size();
        if (size > 0) {
            if (postponedCount == size) {
                return TimeableStatus.POSTPONED;
            } else if (excludedCount == size) {
                return TimeableStatus.EXCLUDED;
            } else if (completedCount + postponedCount + excludedCount == size) {
                return TimeableStatus.COMPLETED;
            } else {
                return TimeableStatus.ACTIVE;
            }
        } else {
            return TimeableStatus.COMPLETED;
        }
    }

    private boolean allChildrenFinalized(RoutineStepEntity step) {
        if (step.children().isEmpty()) return true;
        List<RoutineStepEntity> siblings = step.children();
        return siblings.stream().allMatch(this::isStepFinalized);
    }

    private boolean allSiblingsFinalized(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = (step.parentStep() != null)
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.stream().allMatch(this::isStepFinalized);
    }

    public boolean currentPositionIsFirstStep(RoutineEntity routine) {
        return routine.currentPosition() == 0;
    }

    // ================================
    // Mark Step As <STATUS>
    // ================================

    private void markStepAsCompleted(RoutineStepEntity step, LocalDateTime time) {
        step.status(TimeableStatus.COMPLETED);
        step.finishTime(time);

        if (step.link().isPresent()) {
            step.link(dataContext.mergeNode(new TaskNode(ID.of(step.link().get()))
                    .completed(true)));
        }
    }

    private void unCompleteStep(RoutineStepEntity step, LocalDateTime time) {
        if (step.link().isPresent()) {
            step.link(dataContext.mergeNode(new TaskNode(ID.of(step.link().get()))
                    .completed(false)));
        }

        unPostponeStep(step, time);
    }

    private void markStepAsPostponed(RoutineStepEntity step, LocalDateTime time) {
        step.status(TimeableStatus.POSTPONED);
        step.finishTime(time);

        if (step.link().isPresent() && step.link().get().cron() != null) {
            step.link(dataContext.mergeNode(new TaskNode(ID.of(step.link().get()))
                    .scheduledFor(step.link().get().cron().next(time))));
        }
    }

    private void unPostponeStep(RoutineStepEntity step, LocalDateTime time) {
        if (step.link().isPresent() && step.link().get().cron() != null) {
            step.link(dataContext.mergeNode(new TaskNode(ID.of(step.link().get()))
                    .scheduledFor(time)));
        }
    }

    private void markStepAsSkipped(RoutineStepEntity step, LocalDateTime time) {
        step.status(TimeableStatus.SKIPPED);
        step.finishTime(time);
        step.lastSuspendedTime(time);
    }

    private void markStepAsExcluded(RoutineStepEntity step, LocalDateTime time) {
        step.status(TimeableStatus.EXCLUDED);
        step.finishTime(time);
        step.lastSuspendedTime(time);
    }

    private int markAllChildrenAs(TimeableStatus status, RoutineStepEntity step, LocalDateTime time, int count) {
        for (RoutineStepEntity child : step.children()) {
            count = markAllChildrenAs(status, child, time, count);
            count++;
        }
        switch (status) {
            case POSTPONED -> {
                if(!isStepFinalized(step)) markStepAsPostponed(step, time);
            }
            case SKIPPED -> {
                if(!isStepFinalized(step)) markStepAsSkipped(step, time);
            }
            default -> throw new RuntimeException("Cannot mark children as " + status);
        }
        return count;
    }

    // ================================
    // Helper Handler Methods
    // ================================

    private int processChildren(RoutineStepEntity step, LocalDateTime time, TimeableStatus status) {
        int count = 1;
        // If the step has children
        if (!step.children().isEmpty()) {
            logger.trace("Step " + step.task().name() + " has children. Marking all children as postponed.");
            count = markAllChildrenAs(status, step, time, count);
        }

        return count;
    }

    private RoutineEntity iterateToNextValidStep(RoutineEntity routine, int count, LocalDateTime time) {
        int potentialPosition = routine.currentPosition() + count;

        if (potentialPosition >= routine.countSteps()) {
            if (allSiblingsFinalized(routine.children().get(0))) {
                return completeRoutine(routine);
            } else {
                routine.currentPosition(0);
                return iterateToNextValidStep(routine, 0, time);
            }
        } else {
            RoutineStepEntity current = routine.currentStep();
            if (isLastChild(current) && count > 0 && allChildrenFinalized(current)) {
                if (current.parentStep() != null) {
                    int parentIndex = routine.getAllChildren().indexOf(current.parentStep());
                    routine.currentPosition(parentIndex);
                    return iterateToNextValidStep(routine, 0, time);
                } else {
                    return completeRoutine(routine);
                }
            } else {
                RoutineStepEntity next = routine.getAllChildren().get(potentialPosition);
                if (isStepFinalized(next)) {
                    return iterateToNextValidStep(routine, count + 1, time);
                } else {
                    routine.currentPosition(potentialPosition);
                    activateStep(routine, time);
                    return routine;
                }
            }
        }
    }
}
