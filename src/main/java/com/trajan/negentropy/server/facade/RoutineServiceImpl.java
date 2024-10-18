package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.taskform.fields.EffortConverter;
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
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.model.interfaces.HasTaskLinkOrTaskEntity;
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
import com.trajan.negentropy.util.ServerClockService;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.function.Function;
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
    @PersistenceContext private EntityManager entityManager;

    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationService netDurationService;
    @Autowired private ServerClockService clock;

    @Autowired private DataContext dataContext;

    @Value("${negentropy.refreshRoutines:true}") @Setter @Getter protected boolean refreshRoutines;
    @Value("${negentropy.inTesting:false}") protected boolean inTesting;

    private final AsyncMapBroadcaster<RoutineID, Routine> routineBroadcaster = new AsyncMapBroadcaster<>();

    @PostConstruct
    public void init() {
        log.info("Initializing RoutineService");
        Iterable<RoutineEntity> results = entityQueryService.findRoutines(
                QRoutineEntity.routineEntity.status.ne(TimeableStatus.EXCLUDED)
                        .and(QRoutineEntity.routineEntity.cleaned.isFalse()));
        Set<RoutineEntity> cleanedRoutines = new HashSet<>();
        log.debug("Completed routine repository search");
        for (RoutineEntity routine : results) {
            log.debug("Checking routine {}", routine.id());
            if (routine.status().equals(TimeableStatus.COMPLETED) ||
                routine.status().equals(TimeableStatus.SKIPPED)) {
                if (!routine.cleaned()) {
                    this.cleanUpRoutine(routine, clock.time(), true);
                    routine.cleaned(true);
                    cleanedRoutines.add(routine);
                }
            }
        }

        routineRepository.saveAll(cleanedRoutines);
        effortTracker = new EffortTracker();
    }

    public RoutineID activeRoutineId() {
        Routine activeRoutine = activeRoutine();
        return activeRoutine != null
                ? activeRoutine().id()
                : null;
    }

    public Routine activeRoutine() {
        Iterator<RoutineEntity> iter = entityQueryService.findActiveRoutines().iterator();

        RoutineEntity result = null;
        while (iter.hasNext()) {
            RoutineEntity current = iter.next();
            if (result == null) {
                result = current;
            } else {
                RoutineStepEntity currentStep = current.currentStep();
                RoutineStepEntity lastCurrentStep = result.currentStep();
                if (currentStep.status().equals(TimeableStatus.ACTIVE)
                        && !lastCurrentStep.status().equals(TimeableStatus.ACTIVE)) {
                    result = current;
                } else if (currentStep.startTime().isAfter(lastCurrentStep.startTime())) {
                    result = current;
                }
            }
        }

        return result != null
                ? dataContext.toDO(result)
                : null;
    }

    @Override
    public Routine fetchRoutine(RoutineID routineID) {
        RoutineEntity entity = entityQueryService.getRoutine(routineID);
        return dataContext.toDO(entity);
    }

    @Override
    public RoutineStep fetchRoutineStep(StepID stepID) {
        log.trace("fetchRoutineStep");
        return dataContext.toDO(entityQueryService.getRoutineStep(stepID));
    }

    @Deprecated
    private RoutineResponse process(Supplier<RoutineEntity> routineSupplier) {
        return this.process(routineSupplier, null);
    }

    private synchronized RoutineResponse process(Supplier<RoutineEntity> routineSupplier, StepID focusedStepId) {
        try {
            RoutineEntity routine = routineSupplier.get();
            Optional<String> focusedStepName = routine.descendants().stream()
                    .filter(step -> ID.of(step).equals(focusedStepId))
                    .map(RoutineStepEntity::name)
                    .findFirst();
            Routine routineDO = dataContext.toDO(routine);
            routineBroadcaster.broadcast(ID.of(routine), routineDO);
            String message = focusedStepName.orElse(K.OK);
            return new RoutineResponse(true, routineDO, message);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            log.error("Error processing routine: {}", exceptionAsString);
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
        if (limit.isEmptyWithoutEffort() && link.child().project()) {
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
                    eta,
                    limit.effortMaximum(),
                    custom);
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
                this.refreshRoutine(routine, effortTracker.effort, true);
                routineBroadcaster.broadcast(ID.of(routine), dataContext.toDO(routine));
            });
        }
    }

    @Override
    public void setActiveRoutinesEffort(Integer effort) {
        if (effort == EffortConverter.DEFAULT_EFFORT_INT) effort = null;
        effortTracker.effort = effort;
        if (refreshRoutines) {
            entityQueryService.findActiveRoutines().forEach(routine -> {
                this.refreshRoutine(routine, effortTracker.effort, true);
                routineBroadcaster.broadcast(ID.of(routine), dataContext.toDO(routine));
            });
        }
    }

    @Override
    public synchronized Routine refreshRoutine(RoutineID routineId) {
        return dataContext.toDO(
                this.refreshRoutine(entityQueryService.getRoutine(routineId), effortTracker.effort, true));
    }

    private synchronized RoutineEntity refreshRoutine(RoutineEntity routine, Integer effort, boolean save) {
        if (refreshRoutines) {
            log.info("Refreshing routine : <{}>", routine.id());
            List<RoutineStepEntity> descendants = routine.descendants();

            RoutineStepEntity current = routine.currentStep();
            for (int i = 0; i < descendants.size(); i++) {
                RoutineStepEntity step = descendants.get(i);
                if (i >= routine.currentPosition()
                        || (step.parentStep() != null && step.parentStep().task().project()
                            && Objects.equals(step.parentStep(), current.parentStep()))) {
                    if (step.status().equals(TimeableStatus.LIMIT_EXCEEDED)) {
                        step.status(TimeableStatus.NOT_STARTED);
                    }
                }
            }

            List<Data> roots = routine.children().stream()
                    .map(Data.class::cast)
                    .collect(Collectors.toCollection(ArrayList::new));
            try {
                RoutineLimitFilter filter = (RoutineLimitFilter) SerializationUtil.deserialize(routine.serializedFilter());
                log.debug("Deserialized filter: {}", filter);
                if (filter != null) {
                    log.debug("Updating effort maximum to: {}", effort);
                    routine.serializedFilter(SerializationUtil.serialize(filter.effortMaximum(effort)));
                }
                RoutineRefreshHierarchy hierarchy = new RoutineRefreshHierarchy(routine);
                this.populateRoutineHierarchy(hierarchy, roots, filter, routine.creationTimestamp());
                return hierarchy.routine();
            } catch (IOException | ClassNotFoundException e) {
                this.skipRoutine(ID.of(routine), ServerClockService.now());
                throw new RuntimeException("Could not deserialize filter: " + routine.serializedFilter(), e);
            }
        } else if (routine.status().equals(TimeableStatus.ACTIVE)) {
            log.warn("Not refreshing routine marked as active : <{}>", routine.id());
        }
        log.info("Skipping refresh routine");
        return save ? routineRepository.save(routine) : routine;
    }

    private RoutineEntity initRoutine(List<Data> roots, RoutineLimitFilter filter, LocalDateTime time) {
        log.debug("Populate routine with filter: {}", filter);
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
            filter = new RoutineLimitFilter(filter.durationLimit(), null, filter.etaLimit(), filter.effortMaximum());
        }

        Duration durationLimit = filter.durationLimit();
        //TODO: This is where we determine if limits account for already passed ETA's or are always optimistic!
        // Optimistic:
        // LocalDateTime etaLimit = timeableUtil.getNextFutureTimeOf(time, filter.etaLimit());
        // Non-Optimistic:
        LocalDateTime etaLimit = filter.etaLimit();

        return new RoutineLimiter(durationLimit, stepCountLimit, etaLimit,
                filter.effortMaximum(), filter.isLimiting());
    }

    private RoutineEntityHierarchy populateRoutineHierarchy(RoutineEntityHierarchy hierarchy, List<Data> roots,
                                                            RoutineLimitFilter filter, LocalDateTime time) {
        NetDurationHelper helper = netDurationService.getHelper(filter);
        boolean filterHadCustomStepCount = filter.stepCountLimit() != null;

        RoutineLimiter limit = createRoutineLimiter(roots, filter, time);
        log.debug("Routine limit: {}", limit);

        for (Data root : roots) {
            if (root instanceof TaskEntity) {
                helper.calculateHierarchicalNetDuration((TaskEntity) root, hierarchy, limit);
            } else if (root instanceof TaskLink link) {
                helper.calculateHierarchicalNetDuration(link, hierarchy, mergeLimits(link, limit, time,
                        !(limit.isEmptyWithEffort() && !filterHadCustomStepCount)));
            } else if (root instanceof LimitedDataWrapper limitedData) {
                if (limitedData.data instanceof TaskLink) {
                    limit = mergeLimits((TaskLink) limitedData.data, limit, time, true);
                }
                helper.calculateHierarchicalNetDuration(limitedData, hierarchy, limit);
            } else if (root instanceof RoutineStepEntity step) {
                if (step.link().isPresent()) {
                    helper.calculateHierarchicalNetDuration(step, hierarchy,
                            mergeLimits(step.link().get(), limit, time,
                                    !(limit.isEmptyWithEffort() && !filterHadCustomStepCount)));
                } else {
                    helper.calculateHierarchicalNetDuration(step, hierarchy, limit);
                }
            } else {
                throw new IllegalArgumentException("Root must be either a TaskEntity or a TaskLink.");
            }
        }

        return hierarchy;
    }

    private RoutineEntity persistRoutine(List<Data> roots, RoutineLimitFilter filter, LocalDateTime time) {
        RoutineEntity result = routineRepository.save(initRoutine(roots, filter, time));
        log.debug("Created routine: {} with {} steps.", result.currentStep().name(), result.countSteps());
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
                        entityQueryService.findRoutines(this.filterByStatus(statusSet)).spliterator(), false)
                .map(dataContext::toDO);
    }

    private static String routineLogStatement(RoutineEntity routine) {
        if (!routine.children().isEmpty()) {
            return routine.children().get(0).name() + "<" + routine.id() + ">";
        } else {
            return "<" + routine.id() + "> with no children";
        }
    }

    private void cleanUpRoutine(RoutineEntity routine, LocalDateTime time, boolean delete) {
        log.info("Cleaning up routine: {}", routineLogStatement(routine));
        routine.autoSync(false);
        boolean empty = true;
        List<RoutineStepEntity> descendants = routine.descendants(entityQueryService);
        for (RoutineStepEntity step : descendants) {
            if (step.status().equals(TimeableStatus.ACTIVE) || step.status().equals(TimeableStatus.NOT_STARTED)) {
                step.exclude(time);
            }
            if (step.status().equalsAny(TimeableStatus.COMPLETED, TimeableStatus.POSTPONED)) {
                empty = false;
            }
        }
        if (empty || descendants.isEmpty() || routine.children().isEmpty()) {
            routine.status(TimeableStatus.EXCLUDED);
            if (delete) {
                dataContext.deleteRoutineEntity(routine);
                log.info("Deleting empty routine: {}", routineLogStatement(routine));
            }
        }
    }

    @Override
    public RoutineResponse startStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> activateStep(step, time), stepId);
    }

    @Override
    public RoutineResponse jumpToStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> jumpToStep(step, time), stepId);
    }

    private RoutineEntity jumpToStep(RoutineStepEntity step, LocalDateTime time) {
        RoutineEntity routine = step.routine();
        RoutineStepEntity currentStep = routine.currentStep();

        if (!currentStep.equals(step)) {
            if(!currentStep.status().isFinishedOrExceeded()) {
                RoutineStepEntity farthestActiveAncestor = currentStep;
                while (farthestActiveAncestor.parentStep() != null && farthestActiveAncestor.parentStep().status().equals(TimeableStatus.DESCENDANT_ACTIVE)) {
                    farthestActiveAncestor = farthestActiveAncestor.parentStep();
                }

                markStepAsSkipped(farthestActiveAncestor, time);
                markAllUnfinishedDescendantsAs(TimeableStatus.SKIPPED, farthestActiveAncestor, time);
                routine = farthestActiveAncestor.routine();
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
        boolean first = true;
        while (step != null) {
            log.debug("Activating step <{}> in routine {}.", step.task().name(), routine.id());

            if (first) {
                step.start(time);
                first = false;
            } else {
                step.descendantsStarted(time);
            }
            resetStepLinkStatus(step, time);

            step = step.parentStep();
        }

        if (routine.status().equals(TimeableStatus.NOT_STARTED)) {
            routine.status(TimeableStatus.ACTIVE);
        }

        return routine;
    }

    @Override
    public RoutineResponse jumpToStepAndStartIfReady(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> jumpToStepAndStartIfReady(step, time), stepId);
    }

    private RoutineEntity jumpToStepAndStartIfReady(RoutineStepEntity step, LocalDateTime time) {
        RoutineEntity routine = jumpToStep(step, time);

        if (!step.status().equalsAny(TimeableStatus.COMPLETED, TimeableStatus.POSTPONED)) {
            routine = activateRoutineCurrentStep(routine, time);
        }

        return routine;
    }

    private RoutineStepEntity fetchStepFromActiveRoutine(StepID stepId) {
        try {
            return entityQueryService.getActiveRoutineStep(stepId);
        } catch (NoSuchElementException e) {
            log.warn("Step <{}> not found in active routine.", stepId);
            return entityQueryService.getRoutineStep(stepId);
        }
    }

    @Override
    public boolean completeStepWouldFinishRoutine(StepID stepId) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);
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
                    .allMatch(TimeableStatus::isFinishedOrExceeded);
    }

    @Override
    public RoutineResponse completeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> completeStep(step, time), stepId);
    }

    private RoutineEntity completeStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Completing step <{}> in routine {}.", step.task().name(), step.routine().id());

        if (step.task().project()) {
            switch (getAggregateChildStatus(step)) {
                case COMPLETED, EXCLUDED -> markStepAsCompleted(step, time);
                case LIMIT_EXCEEDED -> markStepAsExceeded(step, time);
                case POSTPONED -> markStepAsPostponed(step, time);
                case ACTIVE, NOT_STARTED, SKIPPED, SUSPENDED -> activateStep(step, time);
                default -> throw new IllegalStateException("Unexpected value: " + getAggregateChildStatus(step));
            }
        } else {
            switch (getAggregateChildStatus(step)) {
                case COMPLETED -> markStepAsCompleted(step, time);
                case POSTPONED -> markStepAsPostponed(step, time);
                case EXCLUDED -> markStepAsExcluded(step, time);
                case LIMIT_EXCEEDED -> markStepAsExceeded(step, time);
                case ACTIVE, NOT_STARTED, SKIPPED, SUSPENDED -> activateStep(step, time);
                default -> throw new IllegalStateException("Unexpected value: " + getAggregateChildStatus(step));
            }
        }

        return refreshAndIterateToNextValidStep(step, time, true, true);
    }

    @Override
    public boolean stepCanBeCompleted(StepID stepId) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        TimeableStatus expectedStatus = getAggregateChildStatus(step);
        return expectedStatus.isFinishedOrExceeded();
    }

    @Override
    public RoutineResponse suspendStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);
        return process(() -> suspendStep(step, time), stepId);
    }

    private RoutineEntity suspendStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Suspending step <{}> in routine {}", step.task().name(), step.routine().id());
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
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> skipStep(step, time), stepId);
    }

    private RoutineEntity skipStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Skipping step <{}> in routine {}.", step.task().name(), step.routine().id());

        markStepAsSkipped(step, time);
        resetStepLinkStatus(step, time);
        markAllUnfinishedDescendantsAs(TimeableStatus.SKIPPED, step, time);

        return refreshAndIterateToNextValidStep(step, time, false, true);
    }

    @Override
    public RoutineResponse postponeStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> postponeStep(step, time), stepId);
    }

    @Override
    public RoutineResponse excludeStep(StepID stepId, LocalDateTime time) {
        return setStepExcluded(stepId, time, true);
    }

    private RoutineEntity postponeStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Postponing step <{}> in routine {}.", step.task().name(), step.routine().id());

        markStepAsPostponed(step, time);

        return refreshAndIterateToNextValidStep(step, time, false, true);
    }

    @Override
    public RoutineResponse previousStep(StepID stepId, LocalDateTime time) {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);

        return process(() -> previousStep(step, time), stepId);
    }

    private RoutineEntity previousStep(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Going to previous step of <{}> in routine {}", step.task().name(), step.routine().id());

        RoutineEntity routine = step.routine();

        if (currentPositionIsFirstStep(routine)) {
            throw new IllegalStateException("Cannot go to previous step when at the first step of a routine.");
        }

        return jumpToStep(routine.descendants().get(routine.currentPosition() - 1), time);
    }

    @Override
    public RoutineResponse skipRoutine(RoutineID routineId, LocalDateTime time) {
        return process(() -> {
            RoutineEntity routine = entityQueryService.getRoutine(routineId);

            routine.status(TimeableStatus.SKIPPED);
            routine.currentStep().status(TimeableStatus.SKIPPED);
            routine.currentStep().finishTime(time);
            cleanUpRoutine(routine, time, true);
            return routine;
        });
    }

    private RoutineEntity completeRoutine(RoutineEntity routine, LocalDateTime time) {
        log.debug("Completing routine {}.", routine.id());
        routine.currentPosition(0);
        routine.status(TimeableStatus.COMPLETED);
        cleanUpRoutine(routine, time, false);
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
    public RoutineResponse setStepExcluded(StepID stepId, LocalDateTime time, boolean exclude)  {
        RoutineStepEntity step = fetchStepFromActiveRoutine(stepId);
        RoutineEntity routine = step.routine();

        log.debug("Setting step {} in routine {} as excluded: {}", step, routine.id(), exclude);

        return process(() -> setStepExcluded(step, time, exclude), stepId);
    }

    private RoutineEntity setStepExcluded(RoutineStepEntity step, LocalDateTime time, boolean exclude) {
        return setStepExcluded(step, time, exclude, true);
    }

    private RoutineEntity setStepExcluded(RoutineStepEntity step, LocalDateTime time, boolean exclude, boolean activateNextStep) {
        if (exclude) {
            if (step.status().equals(TimeableStatus.ACTIVE)) suspendStep(step, time);
            markStepAsExcluded(step, time);
            resetStepLinkStatus(step, time);

            markAllUnfinishedDescendantsAs(TimeableStatus.EXCLUDED, step, time);
            refreshAndIterateToNextValidStep(step, time, false, activateNextStep);
        } else {
            markStepAsSkipped(step, time);
            markAllUnfinishedDescendantsAs(TimeableStatus.SKIPPED, step, time);
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
        RoutineStepEntity initialStep = fetchStepFromActiveRoutine(stepId);
        log.debug("Kicking step {} in routine {} up.", initialStep, initialStep.routine().id());

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

                return entityQueryService.getRoutine(ID.of(routine));
            } else {
                log.debug("Shifting via task");
                HasTaskLinkOrTaskEntity entity = updatedStep.link().isPresent()
                        ? updatedStep.link().get()
                        : updatedStep.task();

                try {
                    TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());
                    Integer effort = null;

                    if (filter instanceof RoutineLimitFilter limitFilter) {
                        effort = limitFilter.effortMaximum();
                    }

                    return routineRepository.save((parent.isEmpty())
                            ? RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineAsRoot(
                                    null,
                                    filter,
                                    routine,
                                    entity,
                                    effort)
                            : RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineStep(
                                    null,
                                    filter,
                                    parent.get(),
                                    entity,
                                    effort));
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Could not deserialize filter for routine " + routine.id(), e);
                }
            }
        }, stepId);
    }

    @Override
    public RoutineResponse pushStepForward(StepID stepId, LocalDateTime time) {
        RoutineStepEntity initialStep = fetchStepFromActiveRoutine(stepId);
        log.debug("Pushing step{} in routine {} up.", initialStep, initialStep.routine().id());
        
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
                return entityQueryService.getRoutine(ID.of(routine));
            } else {
                HasTaskLinkOrTaskEntity entity = updatedStep.link().isPresent()
                        ? updatedStep.link().get()
                        : updatedStep.task();

                try {
                    TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());
                    Integer effort = null;

                    if (filter instanceof RoutineLimitFilter limitFilter) {
                        effort = limitFilter.effortMaximum();
                    }

                    return routineRepository.save((parent.isEmpty())
                            ? RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineAsRoot(
                                nextStep.position() + 1,
                                filter,
                                routine,
                                entity,
                                effort)
                            : RoutineStepHierarchy.integrateTaskOrTaskLinkIntoRoutineStep(
                                nextStep.position() + 1,
                                filter,
                                parent.get(),
                                entity,
                                effort));
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("Could not deserialize filter for routine " + routine.id(), e);
                }
            }
        }, stepId);
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

    private boolean isLastChild(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = step.parentStep() != null
                ? step.parentStep().children()
                : step.routine().children();
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
                if (statusSet.contains(status)) {
                    log.debug("Aggregate child status for <{}>  as {}", step.name(), status);
                    return status;
                }
            }
        }

        return (step.status().isFinishedOrExceeded()) ? step.status() : TimeableStatus.COMPLETED;
    }

    private boolean allChildrenFinishedOrExceeded(RoutineStepEntity step) {
        if (step.children().isEmpty()) return true;
        List<RoutineStepEntity> siblings = step.children();
        return siblings.stream()
                .map(RoutineStepEntity::status)
                .allMatch(TimeableStatus::isFinishedOrExceeded);
    }

    private boolean allSiblingsAndSelfFinishedOrExceeded(RoutineStepEntity step) {
        List<RoutineStepEntity> siblings = (step.parentStep() != null)
                ? step.parentStep().children()
                : step.routine().children();
        return siblings.stream()
                .map(RoutineStepEntity::status)
                .allMatch(TimeableStatus::isFinishedOrExceeded);
    }

    public boolean currentPositionIsFirstStep(RoutineEntity routine) {
        return routine.currentPosition() == 0;
    }

    // ================================
    // Mark Step As <STATUS>
    // ================================

    private void markStepAsCompleted(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <{}> as completed.", step.task().name());
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
        log.debug("Marking step <{}> as postponed.", step.task().name());

        step.postpone(time);

        if (step.link().isPresent() && step.link().get().cron() != null) {
            step.link(dataContext.merge(new TaskNode(ID.of(step.link().get()))
                    .scheduledFor(step.link().get().cron().next(time))));
        }
    }

    private void markStepAsSkipped(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <{}> as skipped.", step.task().name());

        step.skip(time);
    }

    private void markStepAsExcluded(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <{}> as excluded.", step.task().name());

        step.exclude(time);
    }

    private void markStepAsExceeded(RoutineStepEntity step, LocalDateTime time) {
        log.debug("Marking step <{}> as exceeded.", step.task().name());

        step.exclude(time);
        step.status(TimeableStatus.LIMIT_EXCEEDED);
    }

    private void markAllUnfinishedDescendantsAs(TimeableStatus status, RoutineStepEntity ancestorStep, LocalDateTime time) {
        Function<RoutineStepEntity, Boolean> filterStepsByStatus = switch (status) {
            case EXCLUDED -> s -> !s.status().isFinished();
            case SKIPPED -> s -> !s.status().isFinishedOrExceeded() && !s.status().equals(TimeableStatus.NOT_STARTED);
            default -> throw new IllegalArgumentException("Cannot mark step as " + status);
        };

        DFSUtil.traverse(ancestorStep).stream()
                .filter(filterStepsByStatus::apply)
                .forEach(s -> {
                    switch (status) {
                        case SKIPPED -> markStepAsSkipped(s, time);
                        case EXCLUDED -> markStepAsExcluded(s, time);
                        default -> throw new IllegalArgumentException("Cannot mark step as " + status);
                    }
                });
    }

    // ================================
    // Helper Handler Methods
    // ================================

    private RoutineEntity refreshAndIterateToNextValidStep(RoutineStepEntity step, LocalDateTime time, boolean includingChildren, boolean activateNextStep) {
        RoutineEntity routine = refreshRoutine(step.routine(), effortTracker.effort, false);
        RoutineStepEntity currentStep = routine.currentStep();
        if (!currentStep.equals(step)) {
            log.warn("Current step is not the same as the step passed to iterateToNextValidStep. Current step: <{}>, passed step: <{}>", currentStep.name(), step.name());
            List<RoutineStepEntity> descendants = DFSUtil.traverse(step);
            if (descendants.contains(currentStep)) {
                step.routine().currentPosition(routine.descendants().indexOf(step));
            } else {
                return routine;
            }
        }
        return goToNextValidStep(routine, time, includingChildren, activateNextStep);
    }

    private int getNextPosition(RoutineEntity routine, boolean includingChildren) {
        RoutineStepEntity current = routine.currentStep();
        int potentialPosition;
        if (!includingChildren) {
            List<RoutineStepEntity> parentChildren = current.parentStep() != null
                    ? current.parentStep().children()
                    : routine.children();
            int nextSiblingIndex = parentChildren.indexOf(current) + 1;
            if (nextSiblingIndex >= parentChildren.size()) {
                return -1;
            }
            RoutineStepEntity nextSibling = parentChildren.get(nextSiblingIndex);
            potentialPosition = routine.descendants().indexOf(nextSibling);
        } else {
            potentialPosition = routine.currentPosition() + 1;
        }
        return potentialPosition;
    }

    /*
    - Go to next valid step, ignoring child of self
        If this step is the last child
            Go up one level
        Else
            Set routine's current position to next sibling
            If the new current is finished, iterate forward including children
            Else, activate the new current step
    */
    /*
    - Go to next valid step, including children of self
        If this step is the last child
            Go up one level
        Else
            Set routine's current position to current position plus one
            If the new current is finished, iterate forward including children
            Else, activate the new current step
    */
    private RoutineEntity goToNextValidStep(RoutineEntity routine, LocalDateTime time, boolean includingChildren, boolean activateNextStep) {
        RoutineStepEntity current = routine.currentStep();
        String including = includingChildren ? "including" : "excluding";
        log.debug("Going to next valid step from <{}> {} children", current.name(), including);
        if (isLastChild(current)
                && allChildrenFinishedOrExceeded(current)) {
            return goUpToParentStep(routine, time, activateNextStep);
        } else {
            int potentialPosition = getNextPosition(routine, includingChildren);
            if (potentialPosition > routine.descendants().size() || potentialPosition == -1) {
                return goUpToParentStep(routine, time, activateNextStep);
            } else {
                routine.currentPosition(potentialPosition);
                return checkNextStepIsValid(routine, time, activateNextStep);
            }
        }
    }

    /*
    - Go up one level
        If the parent is null
            If all routine direct children are finished, complete routine
            Else go to first child
        Else
            Set routine current position to parent step's position
     */
    private RoutineEntity goUpToParentStep(RoutineEntity routine, LocalDateTime time, boolean activateNextStep) {
        RoutineStepEntity current = routine.currentStep();
        log.debug("Going up to parent step from <{}>", current.name());
        if (current.parentStep() == null) {
            if (allSiblingsAndSelfFinishedOrExceeded(routine.children().get(0))) {
                return completeRoutine(routine, time);
            } else {
                routine.currentPosition(0);
                return checkNextStepIsValid(routine, time, activateNextStep);
            }
        } else {
            List<RoutineStepEntity> siblings = current.parentStep().children();
            for (RoutineStepEntity sibling : siblings.subList(current.position(), siblings.size()-1)) {
                if (!sibling.status().isFinishedOrExceeded()) {
                    routine.currentPosition(routine.descendants().indexOf(sibling));
                    return checkNextStepIsValid(routine, time, activateNextStep);
                }
            }

            routine.currentStep(current.parentStep());
            RoutineStepEntity parent = routine.currentStep();
            if (!parent.status().isFinishedOrExceeded()) {
                if (parent.link().isPresent() && parent.link().get().skipToChildren()) {
                    return completeStep(parent, time);
                } else {
                    return activateNextStep ?
                            activateRoutineCurrentStep(routine, time)
                            : routine;
                }
            } else {
                return goUpToParentStep(routine, time, activateNextStep);
            }
        }
    }

    private RoutineEntity checkNextStepIsValid(RoutineEntity routine, LocalDateTime time, boolean activateNextStep) {
        if (routine.currentPosition() >= routine.descendants().size()) {
            if (allSiblingsAndSelfFinishedOrExceeded(routine.children().get(0))) {
                return completeRoutine(routine, time);
            } else {
                routine.currentPosition(0);
                return checkNextStepIsValid(routine, time, activateNextStep);
            }
        }

        RoutineStepEntity current = routine.currentStep();
        log.debug("Checking if next step is valid: <{}>, {}", current.name(), current.status());
        if (current.status().isFinishedOrExceeded()) {
            return goToNextValidStep(routine, time, false, activateNextStep);
        } else {
            if (current.link().isPresent()
                    && current.status().equals(TimeableStatus.NOT_STARTED)
                    && current.link().get().skipToChildren()
                    && !current.children().isEmpty()) {
                return this.completeStep(current, time);
            } else {
                return activateNextStep ?
                        activateRoutineCurrentStep(routine, time)
                        : routine;
            }
        }
    }

    // ================================
    // Routine Recalculator
    // ================================

    private class RoutineSynchronizer {
        private final LinkedHashMap<RoutineID, RoutineEntity> toBroadcast = new LinkedHashMap<>();
        // TODO: Use this for calculating routine positions right before broadcast
        //  private final Map<RoutineStepEntity, RoutineEntity> currentStepMap = new HashMap<>();

        private void insertNewStep(TaskNode newNode) {
            entityQueryService.findRoutineStepsInCurrentRoutinesContainingTask(newNode.parentId()).stream()
                    .forEach(parent -> {
                        RoutineEntity routine = entityQueryService.getRoutine(ID.of(parent.routine()));
                        log.debug("Inserting new step <{}> into routine <{}>", newNode.name(), routine.id());
                        try {
                            RoutineStepEntity current = routine.currentStep();

                            TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(routine.serializedFilter());
                            Integer effort = null;

                            if (filter instanceof RoutineLimitFilter limitFilter) {
                                effort = limitFilter.effortMaximum();
                            }

                            List<TaskLink> linkSiblings = entityQueryService.findChildLinks(newNode.parentId(), filter)
                                    .toList();

                            int linkPosition = linkSiblings.stream()
                                    .map(ID::of)
                                    .toList()
                                    .indexOf(newNode.id());
                            boolean isSibling = linkPosition != -1;

                            if (isSibling) {
                                log.debug("New step <{}> is a valid sibling.", newNode.name());

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
                                        linkSiblings.get(linkPosition),
                                        effort);

                                for (int i = stepPosition; i < parent.children().size(); i++) {
                                    parent.children().get(i).position(i);
                                }

                                while (parent != null) {
                                    if (parent.status().isFinishedOrExceeded()) {
                                        LocalDateTime finishTime = parent.finishTime() == null
                                                ? clock.time()
                                                : parent.finishTime();
                                        parent.suspend(finishTime);
                                        resetStepLinkStatus(parent, finishTime);
                                    }
                                    parent = parent.parentStep();
                                }
                            }

                            int positionOfCurrent = routine.descendants().indexOf(current);
                            routine.currentPosition((positionOfCurrent != -1)
                                    ? positionOfCurrent
                                    : 0);

                            toBroadcast.put(ID.of(routine), routineRepository.save(routine));
                        } catch (IOException | ClassNotFoundException e) {
                            throw new RuntimeException("Could not deserialize filter for routine " + routine.id(), e);
                        }
                    });
        }

        private void removeStepsContaining(LinkID deletedId) {
            entityQueryService.findRoutineStepsIdsInCurrentRoutinesContainingLink(deletedId).stream()
                    .map(id -> routineStepRepository.getReferenceById(id.val()))
                    .forEach(step -> {
                        step.link(null);
                        this.removeStep(step);
                    });
        }

        private void removeStep(RoutineStepEntity step) {
            log.debug("Removing step <{}> from routine {}.", step.task().name(), step.routine().id());

            RoutineEntity routine = refreshAndIterateToNextValidStep(step, clock.time(), false, false);
            RoutineStepEntity currentStep = routine.currentStep();

            Optional<RoutineStepEntity> updatedStepOptional = routine.descendants().stream()
                    .filter(s -> s.id().equals(step.id()))
                    .findFirst();

            if (updatedStepOptional.isPresent()) {
                RoutineStepEntity updatedStep = updatedStepOptional.get();
                // Get parent step from updated routine
                Ancestor<RoutineStepEntity> parent = routine.descendants().stream()
                        .filter(s -> s.id().equals(updatedStep.parentStep().id()))
                        .findFirst()
                        .orElse(null);
                if (parent == null) parent = routine;

                parent.children().remove(updatedStep);
                for (int i = updatedStep.position(); i < parent.children().size(); i++) {
                    parent.children().get(i).position(i);
                }
                updatedStep.position(null);
                updatedStep.parentStep(null);
                updatedStep.routine(null);
            }

            int positionOfCurrent = routine.descendants().indexOf(currentStep);
            routine.currentPosition((positionOfCurrent != -1)
                    ? positionOfCurrent
                    : 0);

            toBroadcast.put(ID.of(routine), routineRepository.save(routine));
            updatedStepOptional.ifPresent(s ->  routineStepRepository.save(s));
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
                    RoutineEntity routine = entityQueryService.getRoutine(ID.of(step.routine()));
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
                    Stream<RoutineStepEntity> results;
                    if (change.data() instanceof Task task) {
                        results = entityQueryService.findRoutineStepsInCurrentRoutinesContainingTask(task.id())
                                .stream();
                    } else if (change.data() instanceof TaskNode node) {
                        results = entityQueryService.findRoutineStepsInCurrentRoutinesContainingLink(node.id())
                                .stream();
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
                        // Do nothing - this should be handled by the DataContextImpl
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
                    entityQueryService.findRoutineStepsInCurrentRoutinesContainingLink(change.linkId()).stream()
                            .forEach(this::mergeOrRemove);
                } else if (c instanceof InsertRoutineStepChange change) {
                    // TODO: Not yet implemented
                }
            });
        }

        public void broadcast() {
            toBroadcast.forEach((routineId, routine) -> routineBroadcaster.broadcast(
                    routineId, dataContext.toDO(routine)));
        }
    }

    @Override
    public synchronized void notifyChanges(Request request, MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults) {
        RoutineSynchronizer routineSynchronizer = new RoutineSynchronizer();
        routineSynchronizer.process(request, dataResults);
        routineSynchronizer.toBroadcast.get(activeRoutineId());

        routineSynchronizer.broadcast();
    }

    @Override
    public boolean hasFilteredOutSteps(RoutineID routineId) {
        try {
            TaskNodeTreeFilter filter = (TaskNodeTreeFilter) SerializationUtil.deserialize(entityQueryService.getRoutine(routineId)
                    .serializedFilter());
            if (filter instanceof RoutineLimitFilter limitFilter) {
                return limitFilter.effortMaximum() != null && limitFilter.effortMaximum() > 0;
            } else return (!filter.excludedTagIds().isEmpty()
                    || !filter.includedTagIds().isEmpty());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Could not deserialize filter for routine " + routineId, e);
        }
    }

    // ================================
    // Effort Tracker
    // Transitional class to just track effort across all scopes for now
    // ================================

    private class EffortTracker {
        Integer effort = null;

        public EffortTracker() {}
    }

    private EffortTracker effortTracker;

    @Override
    public Integer effortMaximum() {
        return effortTracker.effort;
    }

    @Override
    public void setEffortMaximum(Integer effortMaximum) {
        log.info("Setting effort maximum to " + effortMaximum);
        effortTracker.effort = effortMaximum;
    }
}