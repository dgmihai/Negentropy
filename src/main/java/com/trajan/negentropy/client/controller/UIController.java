package com.trajan.negentropy.client.controller;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.components.grid.TaskTreeGrid;
import com.trajan.negentropy.client.session.RoutineDataProvider;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.HasRootNode;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.sessionlogger.SessionLogged;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;
import com.trajan.negentropy.client.sessionlogger.SessionLoggerFactory;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.backend.util.OrphanTaskCleaner;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringComponent
@UIScope
@Accessors(fluent = true)
@Getter
@Benchmark(millisFloor = 10)
public class UIController implements SessionLogged {
    @Autowired private SessionLoggerFactory loggerFactory;
    protected SessionLogger log;

    @Autowired private UI currentUI;

    @Setter private TaskNodeProvider activeTaskNodeProvider;
    @Setter private HasRootNode activeTaskNodeDisplay;

    @Autowired protected TaskNetworkGraph taskNetworkGraph;

    @Autowired protected SessionServices services;
    @Autowired protected UserSettings settings;

    @Autowired private RoutineDataProvider routineDataProvider;

    @PostConstruct
    public void init() {
        log = getLogger(this.getClass());

        Integer uiId = currentUI != null ? currentUI.getUIId() : null;
        String address = currentUI != null ? currentUI.getSession().getBrowser().getAddress() : null;
        String browser = currentUI != null ? currentUI.getSession().getBrowser().getBrowserApplication() : null;
        log.info("UI client: " + uiId + " at " + address + " using " + browser);
    }

    private <T extends SyncResponse> T tryRequest(Supplier<T> serviceCall) throws Exception {
        T response = serviceCall.get();

        if (currentUI != null) {
            currentUI.access(() -> {
                if (!response.success()) {
                    NotificationMessage.error(response.message());
                } else {
                    NotificationMessage.result(response.message());
                }
            });
        }

        return response;
    }

    private <T extends SyncResponse> T tryRequestAndSync(Supplier<T> serviceCall) throws Exception {
        log.warn("Trying request with manual sync");
        T response = serviceCall.get();

        if (currentUI != null) {
            currentUI.access(() -> {
                if (!response.success()) {
                    NotificationMessage.error(response.message());
                } else {
                    NotificationMessage.result(response.message());
                }
            });
        }

        this.sync();
        return response;
    }

    private DataMapResponse tryDataRequest(Supplier<DataMapResponse> serviceCall, boolean sync) {
        try {
            if (sync) {
                return tryRequestAndSync(serviceCall);
            } else {
                return tryRequest(serviceCall);
            }
        } catch (Throwable t) {
            NotificationMessage.error(t);
            return new DataMapResponse(t.getMessage());
        }
    }

    private SyncResponse trySyncRequest(Supplier<SyncResponse> serviceCall) {
        try {
            return tryRequest(serviceCall);
        } catch (Throwable t) {
            NotificationMessage.error(t);
            return new SyncResponse(t.getMessage());
        }
    }

    //@Override
    @Deprecated
    @VisibleForTesting
    public DataMapResponse requestChange(Change change) {
        return this.requestChanges(List.of(change));
    }

    public void requestChangeAsync(Change change) {
        requestChangeAsync(change, null, null);
    }

    public void requestChangeAsync(Change change, TaskTreeGrid<?> gridRequiringFilterRefresh) {
        requestChangeAsync(change, gridRequiringFilterRefresh, null);
    }

    public void requestChangeAsync(Change change, Consumer<DataMapResponse> callback) {
        requestChangeAsync(change, null, callback);
    }

    public void requestChangeAsync(Change change, TaskTreeGrid<?> gridRequiringFilterRefresh, Consumer<DataMapResponse> callback) {
        log.debug("Request change: " + change);
        CompletableFuture.runAsync(() -> {
            if (callback != null) {
                callback.accept(this.requestChanges(List.of(change)));
            } else {
                this.requestChanges(List.of(change));
            }
        });
    }

    //@Override
    @Deprecated
    @VisibleForTesting
    public DataMapResponse requestChanges(List<Change> changes) {
        log.debug("Requesting " + changes.size() + " changes");
        return this.tryDataRequest(() -> services.change().execute(
                Request.of(taskNetworkGraph.syncId(), changes)), true);
    }

    public void requestChangesAsync(List<Change> changes) {
        requestChangesAsync(changes, null, null);
    }

    public void requestChangesAsync(List<Change> changes, TaskTreeGrid<?> gridRequiringFilterRefresh) {
        requestChangesAsync(changes, gridRequiringFilterRefresh, null);
    }

    public void requestChangesAsync(List<Change> changes, Consumer<DataMapResponse> callback) {
        requestChangesAsync(changes, null, callback);
    }

    public void requestChangesAsync(List<Change> changes, TaskTreeGrid<?> gridRequiringFilterRefresh, Consumer<DataMapResponse> callback) {
        CompletableFuture.runAsync(() -> {
            if (callback != null) {
                callback.accept(this.tryDataRequest(() -> services.change().execute(
                        Request.of(taskNetworkGraph.syncId(), changes)), false));
            } else {
                this.tryDataRequest(() -> services.change().execute(
                        Request.of(taskNetworkGraph.syncId(), changes)), false);
            }
        });
    }

    public synchronized void sync() {
        log.info("Checking for sync");
        SyncResponse response = trySyncRequest(() -> services.query().sync(taskNetworkGraph.syncId()));
        try {
            taskNetworkGraph.sync(response.aggregateSyncRecord());
        } catch (Throwable t) {
            NotificationMessage.error(t);
        }
    }

    private void sync(SyncRecord syncRecord) {
        taskNetworkGraph.sync(syncRecord);
    }

    @Autowired private NetDurationHelperManager netDurationHelperManager;
    //@Override
    public void recalculateNetDurations() {
        log.debug("Recalculating time estimates");
        netDurationHelperManager.recalculateTimeEstimates();
    }

    @Autowired private OrphanTaskCleaner orphanCleaner;

    public void deleteAllOrphanedTasks() {
        log.debug("Deleting orphan tasks");
        try {
            orphanCleaner.deleteAllOrphanedTasks();
            this.sync();
        } catch (Throwable t) {
            t.printStackTrace();
            NotificationMessage.error(t);
        }
    }

    public void deleteAllCompletedTasks() {
        log.debug("Deleting completed tasks");
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter()
                .completed(true);

        requestChangesAsync(services.query().fetchAllNodesAsIds(filter)
                .map(DeleteChange::new)
                .map(change -> (Change) change)
                .toList());
    }

    //==================================================================================================================
    // Routine Management
    //==================================================================================================================

    private RoutineResponse tryRoutineServiceCall(Supplier<RoutineResponse> serviceCall) {
        try {
            RoutineResponse response = serviceCall.get();

            if (currentUI != null) {
                currentUI.access(() -> {
                    if (!response.success()) {
                        NotificationMessage.error(response.message());
                    } else {
                        NotificationMessage.result(response.message());
                        routineDataProvider.refreshAll();
                    }
                });
            }

            return response;
        } catch (Throwable t) {
            NotificationMessage.error(t);
            log.error("Error executing routine service call", t);
            return new RoutineResponse(false, null, t.getMessage());
        }
    }

    public RoutineResponse createRoutine(TaskNode rootNode) {
        log.debug("Creating routine from node: " + rootNode.task().name());
        return tryRoutineServiceCall(() -> services.routine().createRoutine(rootNode.id()));
    }

    public RoutineResponse createRoutine(TaskNode rootNode, TaskNodeTreeFilter filter) {
        log.debug("Creating routine from task: " + rootNode.task().name());
        return tryRoutineServiceCall(() -> services.routine().createRoutine(rootNode.id(), filter));
    }

    public RoutineResponse createRoutine(Task rootTask) {
        log.debug("Creating routine from task: " + rootTask.name());
        return tryRoutineServiceCall(() -> services.routine().createRoutine(rootTask.id()));
    }

    public RoutineResponse createRoutine(Task rootTask, TaskNodeTreeFilter filter) {
        log.debug("Creating routine from task: " + rootTask.name());
        return tryRoutineServiceCall(() -> services.routine().createRoutine(rootTask.id(), filter));
    }

    public RoutineResponse recalculateRoutine(RoutineID routineId) {
        log.debug("Recalculating routine: " + routineId);
        return tryRoutineServiceCall(() -> services.routine().recalculateRoutine(routineId, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse startRoutineStep(StepID stepId) {
        log.debug("Starting routine step: " + stepId);
        return this.tryRoutineServiceCall(
                () -> services.routine().startStep(stepId, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse pauseRoutineStep(StepID stepId) {
        log.debug("Pausing routine step: " + stepId);
        return this.tryRoutineServiceCall(
                () -> services.routine().suspendStep(stepId, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse previousRoutineStep(StepID stepId) {
        log.debug("Going to previous step of routine step: " + stepId);
        return this.tryRoutineServiceCall(
                () -> services.routine().previousStep(stepId, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse completeRoutineStep(StepID stepId) {
        log.debug("Completing routine step: " + stepId);
        return this.tryRoutineServiceCall(
                () -> services.routine().completeStep(stepId, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse skipRoutineStep(StepID stepId) {
        log.debug("Skipping routine step: " + stepId);
        return this.tryRoutineServiceCall(
                () -> services.routine().skipStep(stepId, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse skipRoutine(RoutineID routineId) {
        log.debug("Skipping routine: " + routineId);
        return this.tryRoutineServiceCall(
                () -> services.routine().skipRoutine(routineId, LocalDateTime.now()));
    }

    public RoutineResponse postponeRoutineStep(StepID stepID) {
        log.debug("Postponing routine step: " + stepID);
        return this.tryRoutineServiceCall(
                () -> services.routine().postponeStep(stepID, LocalDateTime.now()));
    }

    //@Override
    public RoutineResponse moveRoutineStep(InsertLocation insertLocation, RoutineStep step, RoutineStep target) {
        throw new NotImplementedException("Not implemented yet");
//        log.debug("Moving routine step: " + step + " " + insertLocation + " " + target);
//
//        StepID parentId = target.parentId();
//
//        int targetPosition;
//        if (parentId != null) {
//            RoutineStep parentStep = services.routine().fetchRoutineStep(target.parentId());
//            targetPosition =  parentStep.children().indexOf(target);
//        } else {
//            Routine routine = services.routine().fetchRoutine(target.routineId());
//            targetPosition = routine.children().indexOf(target);
//        }
//
//        int position = switch(insertLocation) {
//            case BEFORE -> targetPosition - 1;
//            case AFTER -> targetPosition + 1;
//            default -> throw new RuntimeException("Invalid insert location specified.");
//        };
//
//        RoutineResponse response = this.processStep(
//                () -> services.routine().moveStep(step.id(), parentId, position));
//        routineDataProvider.refreshAll();
//        return response;
    }

    //@Override
    public RoutineResponse setRoutineStepExcluded(StepID stepId, boolean exclude) {
        log.debug("Setting routine step excluded: " + stepId + " as " + exclude);
        return this.tryRoutineServiceCall(
                () -> services.routine().setStepExcluded(stepId, LocalDateTime.now(), exclude));
    }
}
