package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineEntityHierarchy;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.*;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.backend.repository.RoutineStepRepository;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    @Autowired private DataContext dataContext;

    @PostConstruct
    public void onStart() {
//        routineRepository.deleteAll();
//        routineStepRepository.deleteAll();
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

    private TaskNodeTreeFilter processFilter(TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter processedFilter = filter != null
                ? filter
                : new TaskNodeTreeFilter();
        processedFilter // We don't cache by name
                .durationLimit(null) // We don't cache by duration limit
                .completed(false)
                .name(null);
        return processedFilter;
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, TaskNodeTreeFilter filter) {
        return this.process(() -> {
            Duration durationLimit = filter != null ? filter.durationLimit() : null;
            TaskNodeTreeFilter processedFilter = processFilter(filter);
            logger.trace("Populate routine with filter: " + processedFilter);

            TaskEntity rootTask = entityQueryService.getTask(rootId);
            RoutineEntity routine = new RoutineEntity();
            RoutineEntityHierarchy hierarchy = new RoutineEntityHierarchy(routine);

            NetDurationHelper helper = netDurationService.getHelper(processedFilter);
            routine.estimatedDuration(helper.getNetDuration(
                    rootTask, hierarchy, durationLimit));

            RoutineEntity result = routineRepository.save(routine);
            logger.debug("Created routine: " + result + " with " + result.countSteps() + " steps.");
            return result;
        });
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId, TaskNodeTreeFilter filter) {
        return this.process(() -> {
            Duration durationLimit = filter != null ? filter.durationLimit() : null;
            TaskNodeTreeFilter processedFilter = processFilter(filter);
            logger.trace("Populate routine with filter: " + processedFilter);

            TaskLink rootLink = entityQueryService.getLink(rootId);
            RoutineEntity routine = new RoutineEntity();
            RoutineEntityHierarchy hierarchy = new RoutineEntityHierarchy(routine);

            NetDurationHelper helper = netDurationService.getHelper(processedFilter);
            routine.estimatedDuration(helper.getNetDuration(
                    rootLink, hierarchy, durationLimit));

            RoutineEntity result = routineRepository.save(routine);
            logger.debug("Created routine: " + result.currentStep().task().name() + " with " + result.countSteps() + " steps.");
            return result;
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

    private RoutineEntity activate(RoutineEntity routine, LocalDateTime time) {
        RoutineStepEntity step = routine.currentStep();
        logger.debug("Starting step " + step.task().name() + " in routine " + routine.id() + ".");
        switch (step.status()) {
            case NOT_STARTED -> {
                step.startTime(time);
                step.lastSuspendedTime(time);
            }
            case SKIPPED, COMPLETED -> {
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

        if (step.startTime() == null) {
            step.startTime(time);
        }

        logger.debug("Activating step " + step.task().name() + " in routine " + routine.id() + ".");
        step.status(TimeableStatus.ACTIVE);
        if (routine.status().equals(TimeableStatus.NOT_STARTED)) {
            routine.status(TimeableStatus.ACTIVE);
        }

        RoutineUtil.setRoutineDuration(routine, time);
        return routine;
    }

    @Override
    public RoutineResponse startStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Starting step " + step + " in routine " + routine.id());

        return process(time, () -> activate(routine, time));
    }

    private void cleanUpRoutine(RoutineEntity routine) {
        for (RoutineStepEntity step : routine.getAllChildren()) {
            if (step.status().equals(TimeableStatus.ACTIVE) || step.status().equals(TimeableStatus.NOT_STARTED)) {
                step.status(TimeableStatus.SKIPPED);
            }
        }
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Suspending step " + step + " in routine " + routine.id());

        return process(time, () -> {
            if (Objects.requireNonNull(step.status()) == TimeableStatus.ACTIVE) {
                step.lastSuspendedTime(time);
                step.status(TimeableStatus.SUSPENDED);
            }

            RoutineUtil.setRoutineDuration(routine, time);
            return step.routine();
        });
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        return process(time, () -> {
            RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
            RoutineEntity routine = step.routine();
            logger.debug("Mark step " + step.task().name() + " as complete in routine " + routine.id() + ".");

            step.status(getStatusBasedOnChildrenStatus(step));
            if (step.status() == TimeableStatus.COMPLETED) {
                markStepAsCompleted(step, time);
            } else if (step.status() == TimeableStatus.SKIPPED) {
                markStepAsSkipped(step, time);
            }

            logger.debug("Completing step " + step.task().name() + " in routine " + routine.id() + ".");

            if (isLastChild(step)) {
                RoutineStepEntity parent = step.parentStep();
                RoutineStepEntity current = step;
                while (parent != null) {
                    parent.status(getStatusBasedOnChildrenStatus(parent));
                    if (current.status() == TimeableStatus.COMPLETED) {
                        markStepAsCompleted(current, time);
                        current = parent;
                        parent = current.parentStep();
                    } else if (current.status() == TimeableStatus.SKIPPED) {
                        markStepAsSkipped(current, time);
                        current = parent;
                        parent = current.parentStep();
                    } else if (current.status() == TimeableStatus.EXCLUDED) {
                        current = parent;
                        parent = current.parentStep();
                    } else {
                        break;
                    }
                }
            }

            if (currentPositionIsLastStep(routine)) {
                return completeRoutine(routine);
            } else {
                routine.currentPosition(routine.currentPosition() + 1);
                activate(routine, time);
            }
            return routine;
        });
    }

    private TimeableStatus getStatusBasedOnChildrenStatus(RoutineStepEntity step) {
        int skippedCount = 0;
        int completedCount = 0;
        int excludedCount = 0;
        for (RoutineStepEntity child : step.children()) {
            switch (child.status()) {
                case SKIPPED -> skippedCount++;
                case COMPLETED -> completedCount++;
                case EXCLUDED -> excludedCount++;
            }
        }
        if (skippedCount > 0) {
            return TimeableStatus.SKIPPED;
        } else if (completedCount == step.children().size()) {
            return TimeableStatus.COMPLETED;
        } else if (excludedCount == step.children().size()) {
            return TimeableStatus.EXCLUDED;
        } else {
            return TimeableStatus.ACTIVE;
        }
    }

    private void markStepAsCompleted(RoutineStepEntity step, LocalDateTime time) {
        step.status(TimeableStatus.COMPLETED);

        if (step.link().isPresent()) {
            boolean completeable = true;
            for (RoutineStepEntity child : step.children()) {
                if (child.link().isPresent()) {
                    if (child.link().get().scheduledFor().isBefore(LocalDateTime.now())
                            || !child.link().get().completed()) {
                        completeable = false;
                    }
                }
            }

            if (completeable) {
                step.link(dataContext.mergeNode(new TaskNode(ID.of(step.link().get()))
                        .completed(true)));
            }
        }

        step.finishTime(time);
    }

    private boolean isLastChild(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = step.parentStep() != null
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.indexOf(step) == siblings.size() - 1;
    }

    @Override
    public RoutineResponse skipStep(StepID stepId, LocalDateTime time) {
        return process(time, () -> {
            RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
            RoutineEntity routine = step.routine();
            logger.debug("Mark step " + step.task().name() + " as skipped in routine " + routine.id() + ".");

            int count = 1;
            // If the step has children
            if (!step.children().isEmpty()) {
                logger.debug("Step " + step.task().name() + " has children. Marking all children as skipped.");
                count = skipAllChildren(step, time, count);
            }

            logger.debug("Skipping step " + step.task().name() + " in routine " + routine.id() + ".");
            markStepAsSkipped(step, time);

            // Check if the current step is the last among its siblings
            if (isLastChild(step)) {
                RoutineStepEntity parent = step.parentStep();
                while (parent != null && allSiblingsSkippedOrCompleted(step)) {
                    markStepAsSkipped(parent, time);
                    step = parent;
                    parent = step.parentStep();
                }
            }

            if (routine.currentPosition() + count >= routine.countSteps()) {
                routine.currentPosition(routine.countSteps() - 1);
                completeRoutine(routine);
            } else {
                routine.currentPosition(routine.currentPosition() + count);
                activate(routine, time);
            }

            return routine;
        });
    }

    private int skipAllChildren(RoutineStepEntity step, LocalDateTime time, int count) {
        for (RoutineStepEntity child : step.children()) {
            count = skipAllChildren(child, time, count);
            count++;
        }
        markStepAsSkipped(step, time);
        return count;
    }

    private void markStepAsSkipped(RoutineStepEntity step, LocalDateTime time) {
        step.status(TimeableStatus.SKIPPED);
        step.finishTime(time);
    }

    private boolean allSiblingsSkippedOrCompleted(RoutineStepEntity step) {
        if (step.parentStep() == null) return false;
        List<RoutineStepEntity> siblings = step.parentStep().children();
        return siblings.stream().allMatch(s -> s.status() == TimeableStatus.SKIPPED || s.status() == TimeableStatus.COMPLETED);
    }

    @Override
    public RoutineResponse previousStep(StepID stepId, LocalDateTime time) {
        return process(time, () -> {
            RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
            RoutineEntity routine = step.routine();

            if (currentPositionIsFirstStep(routine)) {
                throw new IllegalStateException("Cannot go to previous step when at the first step of a routine.");
            }

            logger.debug("Going to previous step of " + step + " in routine " + routine.id());

            // First, suspend the current step
            step.status(TimeableStatus.SUSPENDED);
            if (routine.currentPosition() > 0) {
                routine.currentPosition(routine.currentPosition() - 1);
                RoutineStepEntity previousStep = routine.currentStep();

                // If the previous step was active, restart it
                if (previousStep.status().equals(TimeableStatus.ACTIVE)) {
                    activate(routine, time);
                } else {
                    previousStep.status(TimeableStatus.SUSPENDED);
                }

                if (previousStep.link().isPresent()) {
                    if (previousStep.link().get().completed()) {
                        previousStep.link(dataContext.mergeNode(new TaskNode(ID.of(previousStep.link().get()))
                                .completed(false)));
                    }
                }
            }

            logger.debug("Routine " + routine + " now at position " + routine.currentPosition());

            return routine;
        });
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

        return process(time, () -> {
            if (exclude) {
                step.lastSuspendedTime(time);
                step.status(TimeableStatus.EXCLUDED);
            } else {
                step.status(TimeableStatus.NOT_STARTED);
            }

            RoutineUtil.setRoutineDuration(routine, time);
            return step.routine();
        });
    }

    public boolean currentPositionIsLastStep(RoutineEntity routine) {
        return routine.currentPosition() == routine.countSteps() - 1;
    }

    public boolean currentPositionIsFirstStep(RoutineEntity routine) {
        return routine.currentPosition() == 0;
    }
}
