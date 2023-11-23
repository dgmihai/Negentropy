package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.RoutineStepHierarchy;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineEntityHierarchy;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.*;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.filter.SerializationUtil;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper.RoutineLimiter;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.backend.repository.RoutineStepRepository;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import com.trajan.negentropy.server.broadcaster.AsyncMapBroadcaster;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Transactional
@Slf4j
@Benchmark(millisFloor = 10)
public class RoutineServiceImpl implements RoutineService {

    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationService netDurationService;

    @Autowired private DataContext dataContext;

    @Getter
    private RoutineID activeRoutineId;

    private final AsyncMapBroadcaster<RoutineID, Routine> routineBroadcaster = new AsyncMapBroadcaster<>();

    @PostConstruct
    public void onStart() {
        for (RoutineEntity routine : routineRepository.findAll()) {
            if (routine.status().equals(TimeableStatus.COMPLETED) ||
                routine.status().equals(TimeableStatus.SKIPPED)) {
                this.cleanUpRoutine(routine);
            } else if (routine.status().equals(TimeableStatus.ACTIVE)) {
               if (this.activeRoutineId == null) {
                   this.activeRoutineId = ID.of(routine);
               } else {
                   Routine currentActive = this.fetchRoutine(this.activeRoutineId);
                   try {
                       if (currentActive.currentStep().startTime().isAfter(routine.currentStep().startTime())) {
                           this.activeRoutineId = ID.of(routine);
                       }
                   } catch (NullPointerException e) {
                       log.error("Failed to set active routine", e);
                       // TODO: Address
                   }
               }
            }
        }
    }

    @Override
    public Routine fetchRoutine(RoutineID routineID) {
        log.trace("fetchRoutine");
        return dataContext.toDO(entityQueryService.getRoutine(routineID));
    }

    @Override
    public RoutineStep fetchRoutineStep(StepID stepID) {
        log.trace("fetchRoutineStep");
        return dataContext.toDO(entityQueryService.getRoutineStep(stepID));
    }

    private RoutineResponse process(Supplier<RoutineEntity> routineSupplier) {
        try {
            RoutineEntity routine = routineSupplier.get();
            Routine routineDO = dataContext.toDO(routine);
            routineBroadcaster.broadcast(ID.of(routine), routineDO);
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
    private RoutineEntity initRoutine(List<Data> roots, RoutineLimitFilter filter) {
        log.trace("Populate routine with filter: " + filter);

        RoutineEntity routine = new RoutineEntity();

        try {
            routine.serializedFilter(SerializationUtil.serialize(filter));
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize filter: " + filter, e);
        }

        RoutineEntityHierarchy hierarchy = new RoutineEntityHierarchy(routine);

        NetDurationHelper helper = netDurationService.getHelper(filter);

        RoutineLimiter limit = new RoutineLimiter(filter.durationLimit(), filter.stepCountLimit(), filter.etaLimit(),
                filter.isLimiting());

        for (Data root : roots) {
            if (root instanceof TaskEntity) {
                helper.getNetDuration((TaskEntity) root, hierarchy, limit);
            } else if (root instanceof TaskLink link) {
                if (limit.isEmpty() && link.child().project()) {
                    LocalDateTime eta = link.projectEtaLimit() != null
                            ? link.projectEtaLimit().atDate(LocalDate.now())
                            : null;
                    limit = new RoutineLimiter(link.projectDurationLimit(), link.projectStepCountLimit(),
                            eta, false);
                }
                helper.getNetDuration(link, hierarchy, limit);
            } else {
                throw new IllegalArgumentException("Root must be either a TaskEntity or a TaskLink.");
            }
        }

        return routine;
    }

    private RoutineEntity persistRoutine(List<Data> roots, RoutineLimitFilter filter) {
        RoutineEntity result = routineRepository.save(initRoutine(roots, filter));
        log.debug("Created routine: " + result.currentStep().name() + " with " + result.countSteps() + " steps.");
        activeRoutineId = ID.of(result);
        return result;
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, TaskNodeTreeFilter filter) {
        return this.createRoutine(List.of(rootId), filter);
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId, TaskNodeTreeFilter filter) {
        return this.createRoutine(List.of(rootId), filter);
    }

    @Override
    public RoutineResponse createRoutine(List<TaskOrLinkID> rootIds, TaskNodeTreeFilter filter) {
        return this.process(() -> {
            List<Data> roots = rootIds.stream()
                    .map(id -> {
                        if (id instanceof TaskID taskId) {
                            return entityQueryService.getTask(taskId);
                        } else if (id instanceof LinkID linkId) {
                            return entityQueryService.getLink(linkId);
                        } else {
                            throw new IllegalArgumentException("Root must be either a TaskEntity or a TaskLink.");
                        }
                    })
                    .collect(Collectors.toList());

            return persistRoutine(roots, RoutineLimitFilter.parse(filter));
        });
    }

    private Predicate filterByStatus(Set<TimeableStatus> statusSet) {
        log.trace("filterByStatus");
        QRoutineEntity qRoutine = QRoutineEntity.routineEntity;
        BooleanBuilder builder = new BooleanBuilder();
        statusSet.forEach(status -> builder.or(qRoutine.status.eq(status)));

        return builder;
    }

    @Override
    public long countCurrentRoutines(Set<TimeableStatus> statusSet) {
        log.trace("countCurrentRoutines");
        return routineRepository.count(
                this.filterByStatus(statusSet));
    }

    @Override
    public Stream<Routine> fetchRoutines(Set<TimeableStatus> statusSet) {
        log.trace("fetchRoutines");
        return StreamSupport.stream(
                routineRepository.findAll(this.filterByStatus(statusSet)).spliterator(), false)
                .map(dataContext::toDO);
    }

    private void cleanUpRoutine(RoutineEntity routine) {
        routine.autoSync(false);
        if (activeRoutineId == ID.of(routine)) activeRoutineId = null;
        for (RoutineStepEntity step : routine.getDescendants()) {
            if (step.status().equals(TimeableStatus.ACTIVE) || step.status().equals(TimeableStatus.NOT_STARTED)) {
                step.exclude(null);
            }
        }
    }

    @Override
    public RoutineResponse startStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        activeRoutineId = ID.of(routine);

        return process(() -> activateStep(step, time));
    }

    @Override
    public RoutineResponse jumpToStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        activeRoutineId = ID.of(routine);

        return process(() -> jumpToStep(step, time));
    }

    private RoutineEntity jumpToStep(RoutineStepEntity step, LocalDateTime time) {
        RoutineEntity routine = step.routine();
        RoutineStepEntity currentStep = routine.currentStep();
        if (!currentStep.equals(step)) {
            if(!currentStep.status().isFinished()) {
                markStepAsSkipped(currentStep, time);
                resetStepLinkStatus(currentStep, time);
            }

            List<RoutineStepEntity> steps = routine.getDescendants();
            routine.currentPosition(steps.indexOf(step));
        }
        return routine;
    }

    private RoutineEntity activateStep(RoutineStepEntity step, LocalDateTime time) {
         RoutineEntity routine = jumpToStep(step, time);
         return this.activateStep(routine, time);
    }

    private RoutineEntity activateStep(RoutineEntity routine, LocalDateTime time) {
        RoutineStepEntity step = routine.currentStep();
        log.debug("Activating step <" + step.task().name() + "> in routine " + routine.id() + ".");

        step.start(time);
        resetStepLinkStatus(step, time);

        if (routine.status().equals(TimeableStatus.NOT_STARTED)) {
            routine.status(TimeableStatus.ACTIVE);
        }

        return routine;
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> completeStep(step, time));
    }

    private RoutineEntity completeStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Completing step <" + step.task().name() + "> in routine " + step.routine().id() + ".");

        switch (getStepStatusBasedOnChildrenStatus(step)) {
            case COMPLETED -> markStepAsCompleted(step, time);
            case POSTPONED -> markStepAsPostponed(step, time);
            case SKIPPED -> markStepAsSkipped(step, time);
            case EXCLUDED -> markStepAsExcluded(step, time);
            case ACTIVE -> activateStep(step.routine(), time);
        }

        return iterateToNextValidStep(step, 1, time);
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> suspendStep(step, time));
    }

    private RoutineEntity suspendStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Suspending step <" + step.task().name() + "> in routine " + step.routine().id());

        step.suspend(time);
        resetStepLinkStatus(step, time);

        return step.routine();
    }

    @Override
    public RoutineResponse skipStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> skipStep(step, time));
    }

    private RoutineEntity skipStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Skipping step <" + step.task().name() + "> in routine " + step.routine().id() + ".");

        markStepAsSkipped(step, time);
        resetStepLinkStatus(step, time);

        int count = processChildren(step, time, TimeableStatus.SKIPPED);
        return iterateToNextValidStep(step, count, time);
    }

    @Override
    public RoutineResponse postponeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> postponeStep(step, time));
    }

    @Override
    public RoutineResponse excludeStep(StepID stepId, LocalDateTime time) {
        return setStepExcluded(stepId, time, true);
    }

    private RoutineEntity postponeStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Postponing step <" + step.task().name() + "> in routine " + step.routine().id() + ".");

        TimeableStatus postponedStatus = TimeableStatus.POSTPONED;

        int count = processChildren(step, time, postponedStatus);

        markStepAsPostponed(step, time);

        return iterateToNextValidStep(step, count, time);
    }

    @Override
    public RoutineResponse previousStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> previousStep(step, time));
    }

    private RoutineEntity previousStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Going to previous step of <" + step.task().name() + "> in routine " + step.routine().id());

        RoutineEntity routine = step.routine();

        if (currentPositionIsFirstStep(routine)) {
            throw new IllegalStateException("Cannot go to previous step when at the first step of a routine.");
        }

        markStepAsSkipped(step, time);
        resetStepLinkStatus(step, time);
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
        return process(() -> {
            RoutineEntity routine = entityQueryService.getRoutine(routineId);

            routine.status(TimeableStatus.SKIPPED);
            routine.currentStep().status(TimeableStatus.SKIPPED);
            routine.currentStep().finishTime(time);
            cleanUpRoutine(routine);

            return routine;
        });
    }

    private RoutineEntity completeRoutine(RoutineEntity routine) {
        log.debug("Completing routine " + routine.id() + ".");
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
        activeRoutineId = ID.of(routine);
        log.debug("Setting step " + step + " in routine " + routine.id() + " as excluded: " + exclude);

        return process(() -> setStepExcluded(step, time, exclude));
    }

    private RoutineEntity setStepExcluded(RoutineStepEntity step, LocalDateTime time, boolean exclude) {
        if (exclude) {
            suspendStep(step, time);
            markStepAsExcluded(step, time);
            resetStepLinkStatus(step, time);

            int count = processChildren(step, time, TimeableStatus.EXCLUDED);
            iterateToNextValidStep(step, count, time);
        } else {
            step.status(TimeableStatus.SKIPPED);
        }

        return step.routine();
    }

    @Override
    public RoutineResponse setAutoSync(RoutineID routineId, boolean autoSync) {
        RoutineEntity routine = entityQueryService.getRoutine(routineId);
        routine.autoSync(autoSync);
        return process(() -> routine);
    }

    @Override
    public Registration register(RoutineID routineId, Consumer<Routine> listener) {
        return routineBroadcaster.register(routineId, listener);
    }

    // ================================
    // Boolean Helpers
    // ================================

    private boolean isRoutineFinished(RoutineEntity routine) {
        return (routine.currentPosition() >= routine.countSteps() - 1)
                && (routine.currentStep().status().isFinished());
    }

    private boolean isLastChild(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = step.parentStep() != null
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.indexOf(step) == siblings.size() - 1;
    }

    private TimeableStatus getStepStatusBasedOnChildrenStatus(RoutineStepEntity step) {
        if (allChildrenFinalized(step)) {
            return TimeableStatus.COMPLETED;
        } else {
            return TimeableStatus.ACTIVE;
        }
    }

    private boolean allChildrenFinalized(RoutineStepEntity step) {
        if (step.children().isEmpty()) return true;
        List<RoutineStepEntity> siblings = step.children();
        return siblings.stream()
                .map(RoutineStepEntity::status)
                .allMatch(TimeableStatus::isFinished);
    }

    private boolean allSiblingsFinalized(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = (step.parentStep() != null)
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.stream()
                .map(RoutineStepEntity::status)
                .allMatch(TimeableStatus::isFinished);
    }

    public boolean currentPositionIsFirstStep(RoutineEntity routine) {
        return routine.currentPosition() == 0;
    }

    // ================================
    // Mark Step As <STATUS>
    // ================================

    private void markStepAsCompleted(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <" + step.task().name() + "> as completed.");

        step.complete(time);

        if (step.link().isPresent()) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .completed(true)));
        }
    }

    private void resetStepLinkStatus(RoutineStepEntity step, LocalDateTime time) {
        if (step.link().isPresent() && step.link().get().completed()) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .completed(false)));
        }

        if (step.link().isPresent() && step.link().get().cron() != null && step.link().get().scheduledFor().isAfter(LocalDateTime.now())) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .scheduledFor(time)));
        }
    }

    private void markStepAsPostponed(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <" + step.task().name() + "> as postponed.");

        step.postpone(time);

        if (step.link().isPresent() && step.link().get().cron() != null) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .scheduledFor(step.link().get().cron().next(time))));
        }
    }

    private void markStepAsSkipped(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <" + step.task().name() + "> as skipped.");

        step.skip(time);
    }

    private void markStepAsExcluded(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <" + step.task().name() + "> as excluded.");

        step.exclude(time);
    }

    private int markAllChildrenAs(TimeableStatus status, RoutineStepEntity step, LocalDateTime time, int count) {
        for (RoutineStepEntity child : step.children()) {
            count = markAllChildrenAs(status, child, time, count);
            count++;
        }
        if (!step.status().isFinished()) {
            switch (status) {
                case POSTPONED -> markStepAsPostponed(step, time);
                case SKIPPED -> markStepAsSkipped(step, time);
                case EXCLUDED -> markStepAsExcluded(step, time);
                default -> throw new RuntimeException("Cannot mark children as " + status);
            }
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
            log.trace("Step <" + step.task().name() + "> has children. Marking all children as postponed.");
            count = markAllChildrenAs(status, step, time, count);
        }

        return count;
    }

    private RoutineEntity iterateToNextValidStep(RoutineStepEntity step, int count, LocalDateTime time) {
        RoutineStepEntity currentStep = step.routine().currentStep();
        if (!currentStep.equals(step)) {
            List<RoutineStepEntity> descendants = DFSUtil.traverse(step);
            if (descendants.contains(currentStep)) {
                step.routine().currentPosition(step.routine().getDescendants().indexOf(step));
            } else {
                return step.routine();
            }
        }
        return iterateToNextValidStep(step.routine(), count, time);
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
                    int parentIndex = routine.getDescendants().indexOf(current.parentStep());
                    routine.currentPosition(parentIndex);
                    return iterateToNextValidStep(routine, 0, time);
                } else {
                    return completeRoutine(routine);
                }
            } else {
                RoutineStepEntity next = routine.getDescendants().get(potentialPosition);
                if (next.status().isFinished()) {
                    return iterateToNextValidStep(routine, count + 1, time);
                } else {
                    routine.currentPosition(potentialPosition);
                    activateStep(routine, time);
                    return routine;
                }
            }
        }
    }

    // ================================
    // Routine Recalculator
    // ================================

    @Autowired private EntityManager entityManager;

    private class RoutineSynchronizer {
        private LinkedHashMap<RoutineID, RoutineEntity> toBroadcast = new LinkedHashMap<>();

        private JPAQuery<RoutineStepEntity> onlyActiveRoutinesContainingLink(LinkID linkId) {
            return onlyActiveRoutines()
                    .where(QRoutineStepEntity.routineStepEntity.link.id.eq(linkId.val()));
        }

        private JPAQuery<RoutineStepEntity> onlyActiveRoutinesContainingLinks(Set<LinkID> linkIds) {
            return onlyActiveRoutines()
                    .where(QRoutineStepEntity.routineStepEntity.link.id.in(linkIds.stream()
                            .map(ID::val)
                            .toList()));
        }

        private JPAQuery<RoutineStepEntity> onlyActiveRoutinesContainingTask(TaskID taskId) {
            return onlyActiveRoutines()
                    .where(QRoutineStepEntity.routineStepEntity.task.id.eq(taskId.val()));
        }

        private JPAQuery<RoutineStepEntity> onlyActiveRoutinesContainingTasks(Set<TaskID> taskIds) {
            return onlyActiveRoutines()
                    .where(QRoutineStepEntity.routineStepEntity.task.id.in(taskIds.stream()
                            .map(ID::val)
                            .toList()));
        }

        private JPAQuery<RoutineStepEntity> onlyActiveRoutines() {
            JPAQuery<RoutineStepEntity> query = new JPAQuery<>(entityManager);
            QRoutineStepEntity routineStep = QRoutineStepEntity.routineStepEntity;
            QRoutineEntity routine = QRoutineEntity.routineEntity;

            BooleanBuilder conditions = new BooleanBuilder()
                    .and(routine.autoSync.isTrue())
                    .andAnyOf(routine.status.eq(TimeableStatus.ACTIVE), routine.status.eq(TimeableStatus.NOT_STARTED));

            return query.select(routineStep)
                    .from(routineStep)
                    .join(routineStep.routine, routine)
                    .where(conditions);
        }

        private void insertNewStep(TaskNode newNode) {
            onlyActiveRoutinesContainingTask(newNode.parentId()).fetch()
                    .forEach(step -> {
                        RoutineEntity routine = step.routine();
                        try {
                            TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());
                            int position = entityQueryService.findChildLinks(newNode.parentId(), filter)
                                    .map(ID::of)
                                    .toList()
                                    .indexOf(newNode.id());
                            if (position != -1) {
                                TaskLink link = entityQueryService.getLink(newNode.id());
                                RoutineStepEntity newStep = new RoutineStepEntity()
                                        .link(link)
                                        .task(link.child())
                                        .parentStep(step)
                                        .routine(routine);
                                step.children().add(position, newStep);
                                for (int i = position; i < step.children().size(); i++) {
                                    step.children().get(i).position(i);
                                }
                                if (entityQueryService.hasChildren(newNode.child().id(), filter)) {
                                    RoutineStepHierarchy stepHierarchy = new RoutineStepEntityHierarchy(step);
                                    NetDurationHelper helper = netDurationService.getHelper(filter);
                                    helper.getNetDuration(entityQueryService.getLink(newNode.id()), stepHierarchy, null);
                                }
                                toBroadcast.put(ID.of(routine), routineRepository.save(routine));
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            log.error("Could not deserialize filter: " + routine.serializedFilter(), e);
                        }
                    });
        }

        private void removeStepsContainingDeleted(TaskID deletedId) {
            onlyActiveRoutinesContainingTask(deletedId)
                    .where(QRoutineStepEntity.routineStepEntity.deletedLink.isTrue()).fetch()
                    .forEach(this::removeStep);
        }

        private void removeStepsContaining(LinkID deletedId) {
            onlyActiveRoutinesContainingLink(deletedId).fetch()
                    .forEach(this::removeStep);
        }

        private void removeStep(RoutineStepEntity step) {
            if (!step.status().isFinished()) {
                RoutineEntity routine = step.routine();
                if (routine.currentStep().equals(step)) {
                    routine.currentPosition(routine.currentPosition() - 1);
                }
                RoutineStepEntity parent = step.parentStep();
                parent.children().remove(step);
                for (int i = step.position(); i < parent.children().size(); i++) {
                    parent.children().get(i).position(i);
                }
                step.position(null);
                step.parentStep(null);
                step.routine(null);
                toBroadcast.put(ID.of(routine), routineRepository.save(routine));
                routineStepRepository.save(step);
            }
        }

        private void mergeOrRemove(RoutineStepEntity step) {
            boolean merge = true;
            try {
                TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(step.routine().serializedFilter());
                if (step.link().isPresent()) {
                    LinkID linkId = ID.of(step.link().get());
                    merge = entityQueryService.matchesFilter(linkId, filter);
                } else {
                    TaskID taskId = ID.of(step.task());
                    merge = entityQueryService.matchesFilter(taskId, filter);
                }
            } catch (IOException | ClassNotFoundException e) {
                log.error("Could not deserialize filter: " + step.routine().serializedFilter(), e);
            } finally {
                if (merge) {
                    toBroadcast.put(ID.of(step.routine()), step.routine());
                } else {
                    removeStep(step);
                }
            }
        }

        public void process(Request request, MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults) {
            log.debug("Recalculating routines based on " + request.changes().size() + " changes.");
            LinkedHashMap<RoutineID, RoutineEntity> toBroadcast = new LinkedHashMap<>();
            request.changes().forEach(c -> {
                if (c instanceof MergeChange<?> change) {
                    Iterable<RoutineStepEntity> results;
                    if (change.data() instanceof Task task) {
                        results = onlyActiveRoutinesContainingTask(task.id()).fetch();
                    } else if (change.data() instanceof TaskNode node) {
                        results = onlyActiveRoutinesContainingLink(node.id()).fetch();
                    } else {
                        throw new RuntimeException("Unexpected data type: " + change.data().getClass());
                    }
                    results.forEach(this::mergeOrRemove);
                } else if (c instanceof PersistChange<?> change) {
                    if (change.data() instanceof TaskNodeDTO) {
                        dataResults.get(change.id()).stream()
                                .map(TaskNode.class::cast)
                                .filter(n -> n.parentId() != null)
                                .forEach(this::insertNewStep);
                    }
                } else if (c instanceof DeleteChange<?> change) {
                    if (change.data() instanceof LinkID) {
                        TaskNode deleted = dataResults.get(change.id()).stream()
                                .map(TaskNode.class::cast)
                                .findFirst()
                                .orElseThrow();
                        this.removeStepsContainingDeleted(deleted.childId());
                    }
                } else if (c instanceof InsertChange change) {
                    dataResults.get(change.id()).stream()
                            .map(TaskNode.class::cast)
                            .filter(n -> n.parentId() != null)
                            .forEach(this::insertNewStep);
                } else if (c instanceof MoveChange change) {
                    this.removeStepsContaining(change.originalId());
                    dataResults.get(change.id()).stream()
                            .map(TaskNode.class::cast)
                            .filter(n -> n.parentId() != null)
                            .forEach(this::insertNewStep);
                } else if (c instanceof MultiMergeChange<?, ?> change) {
                    if (change.template() instanceof TaskDTO) {
                        Set<TaskID> taskIds = dataResults.get(change.id()).stream()
                                .map(Task.class::cast)
                                .map(Task::id)
                                .collect(Collectors.toSet());
                        onlyActiveRoutinesContainingTasks(taskIds).fetch()
                                .forEach(this::mergeOrRemove);
                    } else if (change.template() instanceof TaskNodeDTO) {
                        Set<LinkID> linkIds = dataResults.get(change.id()).stream()
                                .map(TaskNode.class::cast)
                                .map(TaskNode::id)
                                .collect(Collectors.toSet());
                        onlyActiveRoutinesContainingLinks(linkIds).fetch()
                                .forEach(this::mergeOrRemove);
                    }
                } else if (c instanceof CopyChange change) {
                    dataResults.get(change.id()).stream()
                            .map(TaskNode.class::cast)
                            .forEach(this::insertNewStep);
                } else if (c instanceof OverrideScheduledForChange change) {
                    onlyActiveRoutinesContainingLink(change.linkId()).fetch()
                            .forEach(this::mergeOrRemove);
                } else if (c instanceof InsertRoutineStepChange change) {
                    // TODO: Not yet implemented
                }

                toBroadcast.forEach((routineId, routine) -> routineBroadcaster.broadcast(routineId,
                        dataContext.toDO(routine)));
            });
        }
    }

    @Override
    public synchronized void notifyChanges(Request request, MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults) {
        new RoutineSynchronizer().process(request, dataResults);
    }
}
