package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.entity.QRoutineEntity;
import com.trajan.negentropy.server.backend.entity.RoutineEntity;
import com.trajan.negentropy.server.backend.entity.RoutineStepEntity;
import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.backend.entity.status.StepStatus;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.RoutineUtil;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
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
    @Autowired private EntityQueryService entityQueryService;

    @Override
    public Routine fetchRoutine(RoutineID routineID) {
        return DataContext.toDTO(entityQueryService.getRoutine(routineID));
    }

    @Override
    public RoutineStep fetchRoutineStep(StepID stepID) {
        return DataContext.toDTO(entityQueryService.getRoutineStep(stepID));
    }

    private RoutineResponse process(
            Supplier<RoutineEntity> routineSupplier) {
        try {
            RoutineEntity routine = routineSupplier.get();
            return new RoutineResponse(true, routine, K.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new RoutineResponse(false, null, e.getMessage());
        }
    }

    @Override
    public RoutineResponse createRoutine(TaskID rootId) {
        return this.createRoutine(rootId, null);
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, TaskFilter filter) {
        return this.process(() -> {
            if (rootId == null) {
                throw new IllegalArgumentException("Task ID to start routine from cannot be null");
            }

            RoutineEntity routine = new RoutineEntity()
                    .estimatedDuration(entityQueryService.calculateTotalDuration(rootId, filter));

            RoutineStepEntity rootStep = new RoutineStepEntity()
                    .task(entityQueryService.getTask(rootId))
                    .routine(routine)
                    .position(0);

            routine.steps().add(rootStep);

            entityQueryService.findDescendantTasks(rootId, filter)
                    .map(task -> new RoutineStepEntity()
                            .task(task))
                    .forEachOrdered(step -> {
                        step.position(routine.steps().size())
                            .routine(routine);
                        routine.steps().add(step);
                    });

            logger.debug("Creating routine " + routine);
            return routineRepository.save(routine);
        });
    }

    private Predicate filterByStatus(Set<RoutineStatus> statusSet) {
        QRoutineEntity qRoutine = QRoutineEntity.routineEntity;
        BooleanBuilder builder = new BooleanBuilder();
        statusSet.forEach(status -> builder.or(qRoutine.status.eq(status)));
        return builder;
    }

    @Override
    public long countCurrentRoutines(Set<RoutineStatus> statusSet) {
        return routineRepository.count(
                this.filterByStatus(statusSet));
    }

    @Override
    public Stream<Routine> fetchRoutines(Set<RoutineStatus> statusSet) {
        return StreamSupport.stream(
                routineRepository.findAll(this.filterByStatus(statusSet)).spliterator(), false)
                .map(DataContext::toDTO);
    }

    private RoutineEntity startStepSupplier(RoutineStepEntity step, RoutineEntity routine, LocalDateTime time) {
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

        step.status(StepStatus.ACTIVE);
        if (routine.status().equals(RoutineStatus.NOT_STARTED)) {
            routine.status(RoutineStatus.ACTIVE);
        }

        RoutineUtil.setRoutineDuration(routine, time);
        return step.routine();
    }

    @Override
    public RoutineResponse startStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Starting step " + step + " in routine " + routine);

        return process(() -> startStepSupplier(step, routine, time));
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Suspending step " + step + " in routine " + routine);

        return process(() -> {
            if (Objects.requireNonNull(step.status()) == StepStatus.ACTIVE) {
                step.lastSuspendedTime(time);
                step.status(StepStatus.SUSPENDED);
            }

            RoutineUtil.setRoutineDuration(routine, time);
            return step.routine();
        });
    }

    private RoutineEntity iterateStepSupplier(
            RoutineStepEntity step, RoutineEntity routine, LocalDateTime time, StepStatus newStatus) {
        switch (step.status()) {
            case SUSPENDED -> {
                step.elapsedSuspendedDuration(
                        Duration.between(step.lastSuspendedTime(), time)
                                .plus(step.elapsedSuspendedDuration()));
                step.status(StepStatus.ACTIVE);
            }
            case ACTIVE -> step.finishTime(time);
        }

        step.status(newStatus);
        if (step.position().equals(routine.steps().size() - 1)) {
            routine.status(RoutineStatus.COMPLETED);
        } else {
            routine.currentPosition(routine.currentPosition() + 1);
        }

        RoutineUtil.setRoutineDuration(routine, time);
        return step.routine();
    }

    private RoutineEntity iterateStep(StepID stepId, LocalDateTime time, StepStatus newStatus) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Iterating step " + step + " to " + newStatus + " in routine " + routine);

        routine = iterateStepSupplier(step, routine, time, newStatus);
        if (!routine.status().equals(RoutineStatus.COMPLETED)) {
            step = routine.steps().get(routine.currentPosition());
            return startStepSupplier(step, routine, time);
        } else {
            return routine;
        }
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        return process(() -> iterateStep(stepId, time, StepStatus.COMPLETED));
    }

    @Override
    public RoutineResponse skipStep(StepID stepId, LocalDateTime time) {
        return process(() -> iterateStep(stepId, time, StepStatus.SKIPPED));
    }

    @Override
    public RoutineResponse previousStep(StepID stepId, LocalDateTime time) {
        return process(() -> {
            RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
            RoutineEntity routine = step.routine();
            boolean previousStatusActive = step.status().equals(StepStatus.ACTIVE);

            logger.debug("Going to previous step of " + step + " in routine " + routine);

            RoutineResponse response = this.suspendStep(stepId, time);
            if (response.success()) {
                logger.debug("1 CURRENT POSITION: " + routine.currentPosition());
                routine.currentPosition(routine.currentPosition() - 1);
                logger.debug("2 CURRENT POSITION: " + routine.currentPosition());
                if (previousStatusActive) {
                    logger.debug("3 CURRENT POSITION: " + routine.currentPosition());
                    routine = startStepSupplier(routine.currentStep(), routine, time);
                    logger.debug("4 CURRENT POSITION: " + routine.currentPosition());
                } else {
                    logger.debug("5 CURRENT POSITION: " + routine.currentPosition());
                    step.status(StepStatus.SUSPENDED);
                    logger.debug("6 CURRENT POSITION: " + routine.currentPosition());
                }
            }
            logger.debug("7 CURRENT POSITION: " + routine.currentPosition());

            RoutineUtil.setRoutineDuration(routine, time);
            return routine;
        });
    }
}
