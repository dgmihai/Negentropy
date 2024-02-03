package com.trajan.negentropy.server.facade;

import com.helger.commons.annotation.VisibleForTesting;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
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
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.model.interfaces.TaskOrTaskLinkEntity;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper;
import com.trajan.negentropy.server.backend.netduration.RefreshHierarchy.RoutineRefreshHierarchy;
import com.trajan.negentropy.server.backend.netduration.RoutineLimiter;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy.RoutineEntityHierarchy;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.backend.repository.RoutineStepRepository;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import com.trajan.negentropy.server.broadcaster.AsyncMapBroadcaster;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Transactional
@Slf4j
@Benchmark
@Accessors(chain = false)
public class RoutineServiceImpl implements RoutineService {

    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationService netDurationService;

    @Autowired private DataContext dataContext;

    @Autowired private TimeableUtil timeableUtil;

    @Value("${negentropy.refreshRoutines:true}") @Setter @Getter protected boolean refreshRoutines;
    @Value("${negentropy.inTesting:false}") protected boolean inTesting;

    @Getter private RoutineID activeRoutineId;

    private final AsyncMapBroadcaster<RoutineID, Routine> routineBroadcaster = new AsyncMapBroadcaster<>();

    private final Map<Routine, LocalDateTime> lastUdated = new HashMap<>();

    @VisibleForTesting
    @Getter @Setter private LocalDateTime manualTime = null;

    @PostConstruct
    public void init() {
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
//                       throw e;
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
        return this.process(routineSupplier, null);
    }

    private synchronized RoutineResponse process(Supplier<RoutineEntity> routineSupplier, StepID focusedStepId) {
        try {
            RoutineEntity routine = routineSupplier.get();
            Routine routineDO = dataContext.toDO(routine);
            String message = (focusedStepId != null)
                    ? routineDO.steps().get(focusedStepId).status().toString()
                    : K.OK;
            routineBroadcaster.broadcast(ID.of(routine), routineDO);
            return new RoutineResponse(true, routineDO, message);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            log.error("Error processing routine: " + exceptionAsString);
            return new RoutineResponse(false, null, e.getMessage());
        }
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, LocalDateTime time) {
        return this.createRoutine(rootId, null, time);
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId, LocalDateTime time) {
        return this.createRoutine(rootId, null, time);
    }

    private RoutineLimiter mergeLimits(TaskLink link, RoutineLimiter limit, LocalDateTime time, boolean custom) {
        if (limit.isEmpty() && link.child().project()) {
            LocalDateTime eta = null;
            if (link.projectEtaLimit().isPresent()) {
                // TODO: Make rollover time into a constant
                LocalTime localEta = link.projectEtaLimit().get();
                //TODO: This is where we determine if limits account for already passed ETA's or are always optimistic!
                // Optimistic:
                // eta = timeableUtil.getNextFutureTimeOf(time, localEta.atDate(time.toLocalDate()));
                // Non-Optimistic:
                eta = localEta.atDate(time.toLocalDate());
            }

            limit = new RoutineLimiter(
                    link.projectDurationLimit().orElse(null),
                    link.projectStepCountLimit().orElse(null),
                    eta, custom);
        }
        return limit;
    }

    public record LimitedDataWrapper(
            Data data,
            Integer stepCountLimit) implements Data {
        @Override
        public String typeName() {
            return this.getClass().getSimpleName();
        }
    }

    @Override
    public void refreshActiveRoutines() {
        if (refreshRoutines) {
            entityQueryService.findActiveRoutines().forEach(routine -> {
                this.refreshRoutine(routine);
                routineBroadcaster.broadcast(ID.of(routine), dataContext.toDO(routine));
            });
        }
    }

    @Override
    public Routine refreshRoutine(RoutineID routineId) {
        return dataContext.toDO(
                this.refreshRoutine(entityQueryService.getRoutine(routineId)));
    }

    private RoutineEntity refreshRoutine(RoutineEntity routine) {
        if (refreshRoutines) {
            log.info("Refreshing routine : <" + routine.id() + ">");
            List<Data> roots = routine.children().stream()
                    .map(step -> (step.link().isPresent())
                            ? step.link().get()
                            : step.task())
                    .map(Data.class::cast)
                    .collect(Collectors.toCollection(ArrayList::new));

            try {
                RoutineLimitFilter filter = (RoutineLimitFilter) SerializationUtil.deserialize(routine.serializedFilter());
                RoutineRefreshHierarchy hierarchy = new RoutineRefreshHierarchy(routine);
                this.populateRoutineHierarchy(hierarchy, roots, filter, routine.creationTimestamp());
                hierarchy.commit();
                return hierarchy.routine();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Could not deserialize filter: " + routine.serializedFilter(), e);
            }
        } else if (routine.status().equals(TimeableStatus.ACTIVE)) {
            log.warn("Not refreshing routine not marked as active : <" + routine.id() + ">");
        }
        log.info("Skipping refresh routine");
        return routine;
    }

    private RoutineEntity initRoutine(List<Data> roots, RoutineLimitFilter filter, LocalDateTime time) {
        log.debug("Populate routine with filter: " + filter);
        RoutineEntity routine = new RoutineEntity().creationTimestamp(time);

        try {
            routine.serializedFilter(SerializationUtil.serialize(filter));
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize filter: " + filter, e);
        }

        return this.populateRoutineHierarchy(
                new RoutineEntityHierarchy(routine), roots, filter, time).routine();
    }

    private RoutineLimiter createRoutineLimiter(List<Data> roots, RoutineLimitFilter filter, LocalDateTime time) {
        // Step Count limit needs to be adjusted in case of a single root
        Integer stepCountLimit = filter.stepCountLimit();
        if (filter.stepCountLimit() != null
                && roots.size() == 1) {
            roots.add(new LimitedDataWrapper(roots.remove(0), filter.stepCountLimit()));
            filter = new RoutineLimitFilter(filter.durationLimit(), null, filter.etaLimit());
        }

        Duration durationLimit = filter.durationLimit();
        //TODO: This is where we determine if limits account for already passed ETA's or are always optimistic!
        // Optimistic:
        // LocalDateTime etaLimit = timeableUtil.getNextFutureTimeOf(time, filter.etaLimit());
        // Non-Optimistic:
        LocalDateTime etaLimit = filter.etaLimit();

        return new RoutineLimiter(durationLimit, stepCountLimit, etaLimit,
                filter.isLimiting());
    }

    private RoutineEntityHierarchy populateRoutineHierarchy(RoutineEntityHierarchy hierarchy, List<Data> roots,
                                                            RoutineLimitFilter filter, LocalDateTime time) {
        NetDurationHelper helper = netDurationService.getHelper(filter);
        boolean filterHadCustomStepCount = filter.stepCountLimit() != null;

        RoutineLimiter limit = createRoutineLimiter(roots, filter, time);
        log.debug("Routine limit: " + limit);

        for (Data root : roots) {
            if (root instanceof TaskEntity) {
                helper.calculateHierarchicalNetDuration((TaskEntity) root, hierarchy, limit);
            } else if (root instanceof TaskLink link) {
                helper.calculateHierarchicalNetDuration(link, hierarchy, mergeLimits(link, limit, time,
                        !(limit.isEmpty() && !filterHadCustomStepCount)));
            } else if (root instanceof LimitedDataWrapper limitedData) {
                if (limitedData.data instanceof TaskLink) {
                    limit = mergeLimits((TaskLink) limitedData.data, limit, time, true);
                }
                helper.calculateHierarchicalNetDuration(limitedData, hierarchy, limit);
            } else {
                throw new IllegalArgumentException("Root must be either a TaskEntity or a TaskLink.");
            }
        }

        return hierarchy;
    }

    private RoutineEntity persistRoutine(List<Data> roots, RoutineLimitFilter filter, LocalDateTime time) {
        RoutineEntity result = routineRepository.save(initRoutine(roots, filter, time));
        log.debug("Created routine: " + result.currentStep().name() + " with " + result.countSteps() + " steps.");
        activeRoutineId = ID.of(result);
        return result;
    }

    @Override
    public RoutineResponse createRoutine(@NotNull TaskID rootId, TaskNodeTreeFilter filter, LocalDateTime time) {
        return this.createRoutine(List.of(rootId), filter, time);
    }

    @Override
    public RoutineResponse createRoutine(@NotNull LinkID rootId, TaskNodeTreeFilter filter, LocalDateTime time) {
        return this.createRoutine(List.of(rootId), filter, time);
    }

    @Override
    public RoutineResponse createRoutine(List<TaskOrLinkID> rootIds, TaskNodeTreeFilter filter, LocalDateTime time) {
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

            return persistRoutine(roots, RoutineLimitFilter.parse(filter), time);
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
        for (RoutineStepEntity step : routine.descendants()) {
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
            if(!currentStep.status().isNotUnfinished()) {
                markStepAsSkipped(currentStep, time);
                resetStepLinkStatus(currentStep, time);
                if (currentStep.parentStep() == null) {
                    routine.children().remove(currentStep.position().intValue());
                    routine.children().add(currentStep.position(), currentStep);
                } else {
                    currentStep.parentStep().children().remove(currentStep.position().intValue());
                    currentStep.parentStep().children().add(currentStep.position(), currentStep);
                }
            }

            List<RoutineStepEntity> steps = routine.descendants();
            routine.currentPosition(steps.indexOf(step));
        }
        return routine;
    }

    private RoutineEntity activateStep(RoutineStepEntity step, LocalDateTime time) {
         RoutineEntity routine = jumpToStep(step, time);
         return this.activateRoutineCurrentStep(routine, time);
    }

    private RoutineEntity activateRoutineCurrentStep(RoutineEntity routine, LocalDateTime time) {
        RoutineStepEntity step = routine.currentStep();
        while (step != null) {
            log.debug("Activating step <" + step.task().name() + "> in routine " + routine.id() + ".");

            step.start(time);
            resetStepLinkStatus(step, time);

            step = step.parentStep();
        }

        if (routine.status().equals(TimeableStatus.NOT_STARTED)) {
            routine.status(TimeableStatus.ACTIVE);
        }

        return routine;
    }

    @Override
    public boolean completeStepWouldFinishRoutine(StepID stepId) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        return completeStepWouldFinishRoutine(step);
    }

    private boolean completeStepWouldFinishRoutine(RoutineStepEntity step) {
        RoutineEntity routine = step.routine();
        ArrayList<RoutineStepEntity> children = new ArrayList<>(routine.children());
        children.addAll(step.children());

        return routine.children().contains(step)
                && routine.currentStep().equals(step)
                && children.stream()
                    .filter(s -> !s.equals(step))
                    .map(RoutineStepEntity::status)
                    .allMatch(TimeableStatus::isNotUnfinished);
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> completeStep(step, time), stepId);
    }

    private RoutineEntity completeStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Completing step <" + step.task().name() + "> in routine " + step.routine().id() + ".");

        if (step.task().project()) {
            switch (getAggregateChildStatus(step)) {
                case COMPLETED, EXCLUDED -> markStepAsCompleted(step, time);
                case POSTPONED -> markStepAsPostponed(step, time);
                case ACTIVE, NOT_STARTED, SKIPPED -> activateStep(step, time);
            }
        } else {
            switch (getAggregateChildStatus(step)) {
                case COMPLETED -> markStepAsCompleted(step, time);
                case POSTPONED -> markStepAsPostponed(step, time);
                case EXCLUDED -> markStepAsExcluded(step, time);
                case ACTIVE, NOT_STARTED, SKIPPED, SUSPENDED -> activateStep(step, time);
            }
        }

        return iterateToNextValidStep(step, 1, time);
    }

    @Override
    public boolean stepCanBeCompleted(StepID stepId) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);

        TimeableStatus expectedStatus = getAggregateChildStatus(step);
        if (step.task().project()) {
            return (expectedStatus.equals(TimeableStatus.COMPLETED) || expectedStatus.equals(TimeableStatus.EXCLUDED));
        } else {
            return expectedStatus.equals(TimeableStatus.COMPLETED);
        }
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> suspendStep(step, time), stepId);
    }

    private RoutineEntity suspendStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Suspending step <" + step.task().name() + "> in routine " + step.routine().id());
        RoutineEntity routine = step.routine();

        while (step != null) {
            step.suspend(time);
            resetStepLinkStatus(step, time);
            step = step.parentStep();
        }

        return routine;
    }

    @Override
    public RoutineResponse skipStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = entityQueryService.getRoutineStep(stepId);
        activeRoutineId = ID.of(step.routine());

        return process(() -> skipStep(step, time), stepId);
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

        return process(() -> postponeStep(step, time), stepId);
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

        return process(() -> previousStep(step, time), stepId);
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
        routine.currentPosition(0);
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

        return process(() -> setStepExcluded(step, time, exclude), stepId);
    }

    private RoutineEntity setStepExcluded(RoutineStepEntity step, LocalDateTime time, boolean exclude) {
        if (exclude) {
            if (step.status().equals(TimeableStatus.ACTIVE)) suspendStep(step, time);
            markStepAsExcluded(step, time);
            resetStepLinkStatus(step, time);

            int count = processChildren(step, time, TimeableStatus.EXCLUDED);
            iterateToNextValidStep(step, count, time);
        } else {
            step.status(TimeableStatus.SKIPPED);
        }

        return step.routine();
    }
    
    private Request getRequestForMovingStep(TaskLink link, 
                                                         LinkID referenceId, 
                                                         InsertLocation location) {
        return Request.of(new InsertAtChange(
                link.toDTO()
                        .cron(null)
                        .recurring(false)
                        .cycleToEnd(false)
                        .positionFrozen(false),
                referenceId,
                location));
    }

    private RoutineEntity setOriginalStepStatusAfterShift(RoutineStepEntity step, LocalDateTime time) {
        RoutineEntity routine;
        if (step.link().isPresent() && step.link().get().recurring() && step.link().get().cron() != null) {
            routine = this.postponeStep(step, time);
        } else {
            routine = this.setStepExcluded(step, time, true);
        }
        return routine;
    }

    @Override
    public RoutineResponse kickStepUp(StepID stepId, LocalDateTime time) {
        RoutineStepEntity initialStep = entityQueryService.getRoutineStep(stepId);
        log.debug("Kicking step" + initialStep + " in routine " + initialStep.routine().id() + " up.");

        return process(() -> {
            Optional<RoutineStepEntity> parent = Optional.ofNullable(initialStep.parentStep());
            Optional<RoutineStepEntity> grandparent = parent.map(RoutineStepEntity::parentStep);

            RoutineEntity routine = setOriginalStepStatusAfterShift(initialStep, time);
            RoutineStepEntity updatedStep = routine.descendants().stream()
                    .filter(step -> step.id().equals(initialStep.id()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find step " + initialStep + " in routine " + routine.id()));

            if (updatedStep.link().isPresent()
                    && parent.isPresent() && parent.get().link().isPresent()
                    && grandparent.isPresent()) {
                log.debug("Shifting via link");
                TaskLink link = updatedStep.link().get();
                ChangeService changeService = SpringContext.getBean(ChangeService.class);
                DataMapResponse response = changeService.execute(getRequestForMovingStep(
                        updatedStep.link().get(),
                        ID.of(parent.get().link().get()),
                        InsertLocation.AFTER));

                if (!response.success()) {
                    throw new RuntimeException("Failed to insert link: " + response.message());
                } else {
                    if (!link.recurring()) {
                        changeService.execute(Request.of(new DeleteChange<>(ID.of(link))));
                    }
                }

                return routineRepository.getReferenceById(routine.id());
            } else {
                log.debug("Shifting via task");
                TaskOrTaskLinkEntity entity = updatedStep.link().isPresent()
                        ? updatedStep.link().get()
                        : updatedStep.task();

                try {
                    TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());

                    return routineRepository.save((parent.isEmpty())
                            ? RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineAsRoot(
                                    null,
                                    filter,
                                    routine,
                                    entity)
                            : RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineStep(
                                    null,
                                    filter,
                                    parent.get(),
                                    entity));
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Could not deserialize filter for routine " + routine.id(), e);
                }
            }
        });
    }

    @Override
    public RoutineResponse pushStepForward(StepID stepId, LocalDateTime time) {
        RoutineStepEntity initialStep = entityQueryService.getRoutineStep(stepId);
        log.debug("Pushing step" + initialStep + " in routine " + initialStep.routine().id() + " up.");
        
        return process(() -> {
            Optional<RoutineStepEntity> parent = Optional.ofNullable(initialStep.parentStep());

            RoutineEntity routine = setOriginalStepStatusAfterShift(initialStep, time);
            RoutineStepEntity updatedStep = routine.descendants().stream()
                    .filter(step -> step.id().equals(initialStep.id()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find step " + initialStep + " in routine " + routine.id()));

            RoutineStepEntity nextStep;
            if (parent.isPresent()) {
                if (parent.get().children().size() > updatedStep.position() + 1) {
                    nextStep = parent.get().children().get(updatedStep.position() + 1);
                } else {
                    throw new UnsupportedOperationException("Task is the last item in its parent's children already.");
                }
            } else {
                if (routine.children().size() > updatedStep.position() + 1) {
                    nextStep = routine.children().get(updatedStep.position() + 1);
                } else {
                    throw new UnsupportedOperationException("Task is at the end of the routine.");
                }
            }

            if (updatedStep.link().isPresent()) {
                if (nextStep.link().isPresent()) {
                    TaskLink link = updatedStep.link().get();
                    ChangeService changeService = SpringContext.getBean(ChangeService.class);
                    DataMapResponse response = changeService.execute(getRequestForMovingStep(
                            updatedStep.link().get(),
                            ID.of(nextStep.link().get()),
                            InsertLocation.AFTER));
                    if (!response.success()) {
                        throw new RuntimeException("Failed to insert link: " + response.message());
                    } else {
                        if (!link.recurring()) {
                            changeService.execute(Request.of(new DeleteChange<>(ID.of(link))));
                        }
                    }
                }
                return routineRepository.getReferenceById(routine.id());
            } else {
                TaskOrTaskLinkEntity entity = updatedStep.link().isPresent()
                        ? updatedStep.link().get()
                        : updatedStep.task();

                try {
                    TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());

                    return routineRepository.save((parent.isEmpty())
                            ? RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineAsRoot(
                                nextStep.position() + 1,
                                filter,
                                routine,
                                entity)
                            : RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineStep(
                                nextStep.position() + 1,
                                filter,
                                parent.get(),
                                entity));
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Could not deserialize filter for routine " + routine.id(), e);
                }
            }
        });
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

    @Override
    public Registration register(Consumer<Routine> listener) {
        return routineBroadcaster.register(listener);
    }

    // ================================
    // Boolean Helpers
    // ================================

    private boolean isRoutineFinished(RoutineEntity routine) {
        return (routine.currentPosition() >= routine.countSteps() - 1)
                && (routine.currentStep().status().isNotUnfinished());
    }

    private boolean isLastChild(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = step.parentStep() != null
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.indexOf(step) == siblings.size() - 1;
    }

    private boolean isLastUnfinishedChild(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = step.parentStep() != null
                ? step.parentStep().children()
                : step.routine().children();
        siblings = siblings.stream()
                .filter(s -> !s.status().isNotUnfinished())
                .collect(Collectors.toList());
        return siblings.indexOf(step) == siblings.size() - 1;
    }

    private TimeableStatus getAggregateChildStatus(RoutineStepEntity step) {
        if (!completeStepWouldFinishRoutine(step)) {
            Set<TimeableStatus> statusSet = step.children().stream()
                    .map(RoutineStepEntity::status)
                    .collect(Collectors.toSet());

            List<TimeableStatus> statusPriority = List.of(
                    TimeableStatus.ACTIVE,
                    TimeableStatus.SUSPENDED,
                    TimeableStatus.SKIPPED,
                    TimeableStatus.NOT_STARTED,
                    TimeableStatus.EXCLUDED,
                    TimeableStatus.COMPLETED,
                    TimeableStatus.POSTPONED,
                    TimeableStatus.LIMIT_EXCEEDED);

            for (TimeableStatus status : statusPriority) {
                if (statusSet.contains(status)) return status;
            }
        }

        return TimeableStatus.COMPLETED;
    }

    private boolean allChildrenNotUnfinished(RoutineStepEntity step) {
        if (step.children().isEmpty()) return true;
        List<RoutineStepEntity> siblings = step.children();
        return siblings.stream()
                .map(RoutineStepEntity::status)
                .allMatch(TimeableStatus::isNotUnfinished);
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
        boolean alreadyCompleted = step.status().equals(TimeableStatus.COMPLETED);

        step.complete(time);

        if (!alreadyCompleted && step.link().isPresent()) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .completed(true), true));
        }
    }

    private void resetStepLinkStatus(RoutineStepEntity step, LocalDateTime time) {
        if (step.link().isPresent() && step.link().get().completed()) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .completed(false)));
        }

        if (step.link().isPresent() && step.link().get().cron() != null && step.link().get().scheduledFor().isAfter(time)) {
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
        if (!step.status().isNotUnfinished()) {
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

    private boolean stepHasLimit(RoutineStepEntity step) {
        if (step.link().isEmpty() || !step.task().project()) return false;
        TaskLink link = step.link().get();
        return link.projectEtaLimit().isPresent()
                || link.projectDurationLimit().isPresent()
                || link.projectStepCountLimit().isPresent();
    }

    private RoutineEntity iterateToNextValidStep(RoutineStepEntity step, int count, LocalDateTime time) {
        RoutineEntity routine = refreshRoutine(step.routine());
        RoutineStepEntity currentStep = routine.currentStep();
        if (!currentStep.equals(step)) {
            log.warn("Current step is not the same as the step passed to iterateToNextValidStep. " +
                    "Current step: <" + currentStep.name() + ">, passed step: <" + step.name() + ">");
            List<RoutineStepEntity> descendants = DFSUtil.traverse(step);
            if (descendants.contains(currentStep)) {
                step.routine().currentPosition(routine.descendants().indexOf(step));
            } else {
                return routine;
            }
        }
        return iterateToNextValidStep(step.routine(), count, time);
    }

    private RoutineEntity iterateToNextValidStep(RoutineEntity routine, int count, LocalDateTime time) {
        int potentialPosition = routine.currentPosition() + count;
        RoutineStepEntity current = routine.currentStep();

        if (potentialPosition >= routine.countSteps() && current.parentStep() == null
                && allSiblingsFinalized(routine.children().get(0))) {
                return completeRoutine(routine);
        } else if (potentialPosition >= routine.countSteps()) {
            int newPosition = routine.currentStep().parentStep() != null
                    ? routine.descendants().indexOf(routine.currentStep().parentStep())
                    : 0;
            routine.currentPosition(newPosition);
            return iterateToNextValidStep(routine, 0, time);
        } else {
            if (isLastChild(current) && count > 0 && allChildrenNotUnfinished(current)) {
                if (current.parentStep() != null) {
                    int parentIndex = routine.descendants().indexOf(current.parentStep());
                    return routine.currentPosition(parentIndex);
                } else {
                    return completeRoutine(routine);
                }
            } else {
                RoutineStepEntity next = routine.descendants().get(potentialPosition);
                if (next.status().isNotUnfinished()) {
                        log.debug("...iterating past step <" + next.task().name() + "> with status " + next.status());
                        return iterateToNextValidStep(routine, count + 1, time);
                } else {
                    routine.currentPosition(potentialPosition);
                    RoutineStepEntity currentStep = routine.currentStep();
                    while (currentStep.link().isPresent()
                            && currentStep.status().equals(TimeableStatus.NOT_STARTED)
                            && currentStep.link().get().skipToChildren()
                            && !currentStep.children().isEmpty()) {
                        routine = this.completeStep(currentStep, time);
                        currentStep = routine.currentStep();
                    }
                    activateRoutineCurrentStep(routine, time);
                    return routine;
                }
            }
        }
    }

    // ================================
    // Routine Recalculator
    // ================================

    private class RoutineSynchronizer {
        private LinkedHashMap<RoutineID, RoutineEntity> toBroadcast = new LinkedHashMap<>();

        private void insertNewStep(TaskNode newNode) {
            entityQueryService.findRoutineStepsInCurrentRoutinesContainingTask(newNode.parentId()).fetch()
                    .forEach(parent -> {
                        RoutineEntity routine = routineRepository.getReferenceById(parent.routine().id());
                        log.debug("Inserting new step <" + newNode.name() + "> into routine <" + routine.id() + ">");
                        try {
                            TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());
                            List<TaskLink> linkSiblings = entityQueryService.findChildLinks(newNode.parentId(), filter)
                                    .toList();

                            int linkPosition = linkSiblings.stream()
                                    .map(ID::of)
                                    .toList()
                                    .indexOf(newNode.id());
                            boolean isSibling = linkPosition != -1;

                            if (isSibling) {
                                log.debug("New step <" + newNode.name() + "> is a valid sibling.");

                                List<TaskLink> linksInRoutineSteps = parent.children().stream()
                                        .map(RoutineStepEntity::link)
                                        .map(o -> o.orElse(null))
                                        .filter(Objects::nonNull)
                                        .toList();

                                int stepPosition = 0;
                                while (linkPosition > 0) {
                                    TaskLink priorLink = linkSiblings.get(linkPosition - 1);
                                    int foundLocation = linksInRoutineSteps.indexOf(priorLink);
                                    if (foundLocation != -1) {
                                        stepPosition = foundLocation + 1;
                                        break;
                                    }
                                    linkPosition--;
                                }

                                routine = RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineStep(
                                        stepPosition,
                                        filter,
                                        parent,
                                        linkSiblings.get(linkPosition));

                                for (int i = stepPosition; i < parent.children().size(); i++) {
                                    parent.children().get(i).position(i);
                                }

                                while (parent != null) {
                                    if (parent.status().isNotUnfinished()) {
                                        LocalDateTime finishTime = parent.finishTime() == null
                                                ? now()
                                                : parent.finishTime();
                                        parent.suspend(finishTime);
                                        resetStepLinkStatus(parent, finishTime);
                                    }
                                    parent = parent.parentStep();
                                }

                                RoutineStepEntity current = routine.currentStep();
                                if (!current.status().equals(TimeableStatus.ACTIVE)
                                        && Objects.equals(current.parentStep(), parent)
                                        && current.position() > stepPosition) {
                                    routine.currentPosition(routine.descendants().indexOf(parent) + stepPosition + 1);
                                }
                            }

                            toBroadcast.put(ID.of(routine), routineRepository.save(routine));
                        } catch (IOException | ClassNotFoundException e) {
                            throw new RuntimeException("Could not deserialize filter for routine " + routine.id(), e);
                        }
                    });
        }

        private void removeStepsContaining(LinkID deletedId) {
            entityQueryService.findRoutineStepsInCurrentRoutinesContainingLink(deletedId).fetch()
                    .forEach(this::removeStep);
        }

        private void removeStep(RoutineStepEntity step) {
            log.debug("Removing step <" + step.task().name() + "> from routine " + step.routine().id() + ".");
            RoutineEntity routine = step.routine();
            if (routine.descendants().indexOf(step) <= routine.currentPosition()) {
                routine.currentPosition(routine.currentPosition() -
                        (1 + step.children().size()));
            }
            Ancestor<RoutineStepEntity>parent = step.parentStep() != null
                    ? step.parentStep()
                    : step.routine();
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
                throw new RuntimeException("Could not deserialize filter for routine " + step.routine().id(), e);
            } finally {
                if (merge) {
                    log.debug("Marking merge of step <" + step.task().name() + "> into routine <" + step.routine().id() + ">.");
                    RoutineEntity routine = routineRepository.getReferenceById(step.routine().id());
                    toBroadcast.put(ID.of(step.routine()), routine);
                } else {
                    removeStep(step);
                }
            }
        }

        public void process(Request request, MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults) {
            log.debug("Recalculating routines based on " + request.changes().size() + " changes.");
            request.changes().forEach(c -> {
                log.debug("> " + c.getClass().getSimpleName());
                if (c instanceof MergeChange<?> change) {
                    Iterable<RoutineStepEntity> results;
                    if (change.data() instanceof Task task) {
                        results = entityQueryService.findRoutineStepsInCurrentRoutinesContainingTask(task.id()).fetch();
                    } else if (change.data() instanceof TaskNode node) {
                        results = entityQueryService.findRoutineStepsInCurrentRoutinesContainingLink(node.id()).fetch();
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
                    if (change.data() instanceof StepID stepId) {
                        this.removeStep(routineStepRepository.getReferenceById(stepId.val()));
                    } else if (change.data() instanceof LinkID linkID) {
                        this.removeStepsContaining(linkID);
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
                    if (change.template() instanceof Task) {
                        Set<TaskID> taskIds = dataResults.get(change.id()).stream()
                                .map(Task.class::cast)
                                .map(Task::id)
                                .collect(Collectors.toSet());
                        entityQueryService.findRoutineStepsInCurrentRoutinesContainingTasks(taskIds).fetch()
                                .forEach(this::mergeOrRemove);
                    } else if (change.template() instanceof TaskNodeDTO) {
                        Set<LinkID> linkIds = dataResults.get(change.id()).stream()
                                .map(TaskNode.class::cast)
                                .map(TaskNode::id)
                                .collect(Collectors.toSet());
                        entityQueryService.findRoutineStepsInCurrentRoutinesContainingLinks(linkIds).fetch()
                                .forEach(this::mergeOrRemove);
                    }
                } else if (c instanceof CopyChange change) {
                    dataResults.get(change.id()).stream()
                            .map(TaskNode.class::cast)
                            .forEach(this::insertNewStep);
                } else if (c instanceof OverrideScheduledForChange change) {
                    entityQueryService.findRoutineStepsInCurrentRoutinesContainingLink(change.linkId()).fetch()
                            .forEach(this::mergeOrRemove);
                } else if (c instanceof InsertRoutineStepChange change) {
                    // TODO: Not yet implemented
                }
            });
        }

        public void broadcast() {
            toBroadcast.forEach((routineId, routine) -> routineBroadcaster.broadcast(routineId,
                    dataContext.toDO(routine)));
        }
    }

    @Override
    public synchronized void notifyChanges(Request request, MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults) {
        RoutineSynchronizer routineSynchronizer = new RoutineSynchronizer();
        routineSynchronizer.process(request, dataResults);
//        Runnable refreshAll = () -> routineSynchronizer.toBroadcast.forEach((routineId, routine) ->
//                routineSynchronizer.toBroadcast.replace(routineId, this.refreshRoutine(routine)));
//        if (inTesting) {
//            refreshAll.run();
//        } else {
//            CompletableFuture.runAsync(refreshAll);
//        }
        routineSynchronizer.broadcast();
    }

    @Override
    public boolean hasFilteredOutSteps(RoutineID routineId) {
        try {
            TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routineRepository.getReferenceById(routineId.val())
                    .serializedFilter());
            return (!filter.excludedTagIds().isEmpty() || !filter.includedTagIds().isEmpty());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Could not deserialize filter for routine " + routineId, e);
        }
    }
}
