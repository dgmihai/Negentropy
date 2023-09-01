package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.Routine;
import com.trajan.negentropy.model.RoutineStep;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.QRoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.RoutineUtil;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Transactional
public class RoutineServiceImpl implements RoutineService {
    private static final Logger logger = LoggerFactory.getLogger(RoutineServiceImpl.class);

    @Autowired private RoutineRepository routineRepository;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private ChangeService changeService;

    @Autowired private DataContext dataContext;

    @PostConstruct
    public void onStart() {
//        for (RoutineEntity routine : routineRepository.findAll()) {
//            if (routine.status().equals(TimeableStatus.COMPLETED) ||
//                routine.status().equals(TimeableStatus.SKIPPED)) {
//                this.cleanUpRoutine(routine);
//            }
//        }
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

    private RoutineResponse process(
            Supplier<RoutineEntity> routineSupplier) {
        try {
            RoutineEntity routine = routineSupplier.get();
            return new RoutineResponse(true, dataContext.toDO(routine), K.OK);
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

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, TaskFilter filter) {
        TaskEntity rootTask = entityQueryService.getTask(rootId);

        // TODO: We create a temporary root task for a routine started without an associated link
        Optional<TaskLink> rootLinkOptional = rootTask.parentLinks().stream()
                .filter(link -> link.parent() == null)
                .findFirst();

        TaskLink rootLink = rootLinkOptional.orElseGet(() -> {
            Change change = new PersistChange<>(new TaskNodeDTO()
                    .parentId(null)
                    .childId(rootId));
            DataMapResponse response = changeService.execute(Request.of(change));
            TaskNode rootNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());
            return entityQueryService.getLink(rootNode.linkId());
        });

        if (filter == null) {
            filter = new TaskFilter();
            filter.durationLimit(ChronoUnit.FOREVER.getDuration());
        }

        return this.createRoutine(ID.of(rootLink), filter);
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId, TaskFilter filter) {
        return this.process(() -> {
            TaskLink rootLink = entityQueryService.getLink(rootId);

            RoutineEntity routine = new RoutineEntity();

            return this.populateRoutine(rootLink, routine, filter);
        });
    }

    private RoutineEntity populateRoutine(TaskLink rootLink, RoutineEntity routine, TaskFilter filter) {
        AtomicReference<Duration> estimatedDurationReference = new AtomicReference<>(
                addStepToRoutine(rootLink, routine));

        logger.trace("Populate routine filter: " + filter);

        Consumer<TaskLink> computeEstimatedDuration = link -> {
            logger.debug("Adding to routine: " + link.child().name());
            Duration estimatedDuration = estimatedDurationReference.get();
            estimatedDurationReference.set(estimatedDuration.plus(addStepToRoutine(link, routine)));
            logger.debug("Estimated duration: " + estimatedDurationReference + " for " + link.child().name());
        };

        TaskFilter filterWithProjects = filter != null
                ? filter
                : new TaskFilter();
        filterWithProjects
                .completed(false)
                .options()
                    .add(TaskFilter.WITH_PROJECT_DURATION_LIMITS);
        if (filterWithProjects.durationLimit() == null) {
            if (rootLink.child().project()) {
                filterWithProjects.durationLimit(rootLink.projectDuration());
            }
        }

        entityQueryService.findDescendantLinks(ID.of(rootLink), filterWithProjects, computeEstimatedDuration);
//                .forEachOrdered(link -> {
//                    logger.trace("For each: " + link.child().name());
//                    TaskEntity task = link.child();
//                    if (task.project()) {
//                        logger.trace("Found task " + task.name() + " as project, project link duration: "
//                                + link.projectDuration());
//                        List<TaskLink> projectLinks = DFSUtil.traverseTaskLinksFromIDWithLimits(
//                                ID.of(task),
//                                taskId -> entityQueryService.findChildLinks(taskId, filter),
//                                taskLink -> ID.of(taskLink.child()),
//                                link.projectDuration(),
//                                t -> {}
//                        );
//                        logger.debug("Adding " + projectLinks.size() + " tasks from project " + task.name() + " to routine.");
//                        projectLinks.forEach(projectLink -> addStepToRoutine(projectLink, routine));
//                    } else {
//                        logger.debug("Adding task " + task.name() + " to routine.");
//                        addStepToRoutine(link, routine);
//                    }
//                });

        routine.estimatedDuration(estimatedDurationReference.get());

        logger.debug("Saving routine " + routine);
        return routineRepository.save(routine);
    }

    private Duration addStepToRoutine(TaskLink link, RoutineEntity routine) {
        RoutineStepEntity step = new RoutineStepEntity()
                .link(link)
                .position(routine.steps().size())
                .routine(routine);
        routine.steps().add(step);
        logger.trace("Added step for task " + link.child().name() + " at position " + (routine.steps().size() - 1));
        return step.duration();
    }


    private Predicate filterByStatus(Set<TimeableStatus> statusSet) {
        logger.trace("filterByStatus");
        QRoutineEntity qRoutine = QRoutineEntity.routineEntity;
        BooleanBuilder builder = new BooleanBuilder();
        statusSet.forEach(status -> builder.or(qRoutine.status.eq(status)));

        // TODO: Due to old routines that still use TaskID
        builder.andNot(qRoutine.steps.get(0).link.isNull());
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

    private RoutineEntity startStepSupplier(RoutineStepEntity step, RoutineEntity routine, LocalDateTime time) {
        switch (step.status()) {
            case NOT_STARTED -> {
                step.startTime(time);
                step.lastSuspendedTime(time);
            }
            case SKIPPED, COMPLETED -> {
                if (step.finishTime() == null) step.finishTime(time); // TODO: Remove when routine stabilized
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

        step.status(TimeableStatus.ACTIVE);
        if (routine.status().equals(TimeableStatus.NOT_STARTED)) {
            routine.status(TimeableStatus.ACTIVE);
        }

        RoutineUtil.setRoutineDuration(routine, time);
        return step.routine();
    }

    @Override
    public RoutineResponse startStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Starting step " + step + " in routine " + routine.id());

        return process(() -> startStepSupplier(step, routine, time));
    }

    private void cleanUpRoutine(RoutineEntity routine) {
        for (RoutineStepEntity step : routine.steps()) {
            if (step.link() != null) {
                step.taskRecord(step.link().child());
            }
            if (step.status().equals(TimeableStatus.ACTIVE) || step.status().equals(TimeableStatus.NOT_STARTED)) {
                step.status(TimeableStatus.SKIPPED);
            }
            step.link(null);
        }
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Suspending step " + step + " in routine " + routine.id());

        return process(() -> {
            if (Objects.requireNonNull(step.status()) == TimeableStatus.ACTIVE) {
                step.lastSuspendedTime(time);
                step.status(TimeableStatus.SUSPENDED);
            }

            RoutineUtil.setRoutineDuration(routine, time);
            return step.routine();
        });
    }

    private RoutineEntity iterateStep(StepID stepId, LocalDateTime time, TimeableStatus newStatus) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Iterating step " + step + " to " + newStatus + " in routine " + routine.id());

        if (Objects.requireNonNull(step.status()) == TimeableStatus.SUSPENDED) {
            step.elapsedSuspendedDuration(
                    Duration.between(step.lastSuspendedTime(), time)
                            .plus(step.elapsedSuspendedDuration()));
        }

        step.finishTime(time);
        step.status(newStatus);
        if (newStatus.equals(TimeableStatus.COMPLETED) &&
                (step.link().cron() != null || step.link().scheduledFor() != null)) {
            changeService.execute(Request.of(new MergeChange<>(dataContext.toDO(step.link())
                    .completed(true))));
        }

        if (step.position().equals(routine.steps().size() - 1)) {
            routine.status(TimeableStatus.COMPLETED);
        } else {
            routine.currentPosition(routine.currentPosition() + 1);
        }

        RoutineUtil.setRoutineDuration(routine, time);

        if (!routine.status().equals(TimeableStatus.COMPLETED)) {
            step = routine.steps().get(routine.currentPosition());
            logger.debug("Routine " + routine + " now at position " + routine.currentPosition());
            return startStepSupplier(step, routine, time);
        } else {
            logger.debug("Routine " + routine + " complete.");
            cleanUpRoutine(routine);
            return routine;
        }
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        return process(() -> iterateStep(stepId, time, TimeableStatus.COMPLETED));
    }

    @Override
    public RoutineResponse skipStep(StepID stepId, LocalDateTime time) {
        return process(() -> iterateStep(stepId, time, TimeableStatus.SKIPPED));
    }

    @Override
    public RoutineResponse previousStep(StepID stepId, LocalDateTime time) {
        return process(() -> {
            RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
            RoutineEntity routine = step.routine();
            if (routine.currentPosition() == 0) {
                throw new IllegalStateException("Cannot go to previous step when at the first step of a routine.");
            }

            boolean previousStatusActive = step.status().equals(TimeableStatus.ACTIVE);

            logger.debug("Going to previous step of " + step + " in routine " + routine.id());

            RoutineResponse response = this.suspendStep(stepId, time);
            if (response.success()) {
                routine.currentPosition(routine.currentPosition() - 1);
                if (previousStatusActive) {
                    routine = startStepSupplier(routine.currentStep(), routine, time);
                } else {
                    step.status(TimeableStatus.SUSPENDED);
                }
            }
            logger.debug("Routine " + routine + " now at position " + routine.currentPosition());

            RoutineUtil.setRoutineDuration(routine, time);
            return routine;
        });
    }

    @Override
    public RoutineResponse skipRoutine(RoutineID routineId, LocalDateTime time) {
        return process(() -> {
            RoutineEntity routine = entityQueryService.getRoutine(routineId);

            routine.status(TimeableStatus.SKIPPED);
            routine.currentStep().status(TimeableStatus.SKIPPED);
            routine.currentStep().finishTime(time);

            routine.steps().stream()
                    .filter(step -> step.status().equals(TimeableStatus.NOT_STARTED))
                    .forEach(step -> step.status(TimeableStatus.SKIPPED));

            cleanUpRoutine(routine);

            return routine;
        });
    }

    @Override
    public RoutineResponse moveStep(StepID childId, StepID parentId, int position) {
        return process(() -> {
            RoutineStepEntity step = entityQueryService.getRoutineStep(childId);
            RoutineEntity routine = step.routine();

            List<RoutineStepEntity> steps;
            if (parentId == null) {
                steps = step.routine().steps();
            } else {
                RoutineStepEntity parentStep = entityQueryService.getRoutineStep(parentId);
                if (!routine.equals(parentStep.routine())) {
                    throw new RuntimeException(step + " and " + parentStep + " do not belong to the same routine.");
                }
                steps = parentStep.children();
            }

            int oldIndex = steps.indexOf(step);

            steps.remove(step);
            if (position > oldIndex) {
                steps.add(position-1, step);
            } else {
                steps.add(position, step);
            }

            for (int i = 0; i < steps.size(); i++) {
                steps.get(i).position(i);
            }

            return routine;
        });
    }

    @Override
    public RoutineResponse setStepExcluded(StepID stepId, LocalDateTime time, boolean exclude) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        RoutineEntity routine = step.routine();
        logger.debug("Setting step " + step + " in routine " + routine.id() + " as excluded: " + exclude);

        return process(() -> {
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
}
