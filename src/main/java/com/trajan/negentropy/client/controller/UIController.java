package com.trajan.negentropy.client.controller;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.components.grid.TaskTreeGrid;
import com.trajan.negentropy.client.controller.util.HasRootNode;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.*;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.backend.util.OrphanTaskCleaner;
import com.trajan.negentropy.server.broadcaster.MapBroadcaster;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringComponent
@UIScope
@Accessors(fluent = true)
@Getter
@Benchmark(millisFloor = 10)
public class UIController {
    private final UILogger log = new UILogger();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired private UIAccessor uiAccessor;

    @Setter private TaskNodeProvider activeTaskNodeProvider;
    @Setter private HasRootNode activeTaskNodeDisplay;

    @Autowired protected TaskNetworkGraph taskNetworkGraph;
    @Autowired protected TaskEntryDataProvider taskEntryDataProvider;

    @Autowired protected SessionServices services;
    @Autowired protected UserSettings settings;

    @Autowired private RoutineDataProvider routineDataProvider;

    @PostConstruct
    public void init() {
        UI currentUI = UI.getCurrent();
        Integer uiId = currentUI != null ? currentUI.getUIId() : null;
        String address = currentUI != null ? currentUI.getSession().getBrowser().getAddress() : null;
        String browser = currentUI != null ? currentUI.getSession().getBrowser().getBrowserApplication() : null;
        log.info("UI client: " + uiId + " at " + address + " using " + browser);
    }

    private <T extends SyncResponse> void handleResponse(T response) {
        executor.execute(() -> uiAccessor.acquire(() -> {
            if (!response.success()) {
                NotificationMessage.error(response.message());
            } else {
                NotificationMessage.result(response.message());
            }
        }));
    }

    private <T extends SyncResponse> T tryRequest(Supplier<T> serviceCall) throws Exception {
        T response = serviceCall.get();
        handleResponse(response);
        return response;
    }

    private <T extends SyncResponse> T tryRequestAndSync(Supplier<T> serviceCall) throws Exception {
        log.warn("Trying request with manual sync");
        T response = serviceCall.get();
        handleResponse(response);
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
        executor.execute(() -> {
            DataMapResponse response = this.tryDataRequest(() -> services.change().execute(
                    Request.of(taskNetworkGraph.syncId(), changes)), false);
            if (callback != null) {
                uiAccessor.acquire(() -> callback.accept(response));
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

    public void deleteAllCompletedTaskNodes() {
        log.debug("Deleting completed tasks");
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter()
                .hasChildren(false)
                .completed(true);

        requestChangesAsync(services.query().fetchAllNodesAsIds(filter)
                .map(DeleteChange::new)
                .map(change -> (Change) change)
                .toList());
    }

    //==================================================================================================================
    // Routine Management
    //==================================================================================================================

    private void tryRoutineServiceCall(Supplier<RoutineResponse> serviceCall,
                                       Consumer<RoutineResponse> onSuccess,
                                       Consumer<RoutineResponse> onFailure) {
        try {
            executor.execute(() -> {
                RoutineResponse response = serviceCall.get();

                uiAccessor.acquire(() -> {
                    if (!response.success()) {
                        NotificationMessage.error(response.message());
                        if (onFailure != null) onFailure.accept(response);
                    } else {
                        NotificationMessage.result(response.message());
                        routineDataProvider.refreshAll();
                        if (onSuccess != null) onSuccess.accept(response);
                    }
                });
            });
        } catch (Throwable t) {
            log.error("Error executing routine service call", t);
            uiAccessor.acquire(() -> NotificationMessage.error(t));
        }
    }

    public void createRoutine(TaskNode rootNode,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        log.debug("Creating routine from node: " + rootNode.task().name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootNode.id()),
                onSuccess, onFailure);
    }

    public void createRoutine(TaskNode rootNode, TaskNodeTreeFilter filter,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        log.debug("Creating routine from task: " + rootNode.task().name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootNode.id(), filter),
                onSuccess, onFailure);
    }

    public void createRoutine(Task rootTask,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        log.debug("Creating routine from task: " + rootTask.name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootTask.id()),
                onSuccess, onFailure);
    }

    public void createRoutine(Task rootTask, TaskNodeTreeFilter filter,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        log.debug("Creating routine from task: " + rootTask.name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootTask.id(), filter),
                onSuccess, onFailure);
    }

    public void recalculateRoutine(RoutineID routineId,
                                   Consumer<RoutineResponse> onSuccess,
                                   Consumer<RoutineResponse> onFailure) {
        log.debug("Recalculating routine: " + routineId);
        tryRoutineServiceCall(() -> services.routine().recalculateRoutine(routineId),
                onSuccess, onFailure);
    }

    public void setAutoSync(RoutineID routineId, boolean autoSync,
                            Consumer<RoutineResponse> onSuccess,
                            Consumer<RoutineResponse> onFailure) {
        log.debug("Setting auto sync for routine: " + routineId + " to " + autoSync);
        tryRoutineServiceCall(() -> services.routine().setAutoSync(routineId, autoSync),
                onSuccess, onFailure);
    }

    public void startRoutineStep(StepID stepId,
                                 Consumer<RoutineResponse> onSuccess,
                                 Consumer<RoutineResponse> onFailure) {
        log.debug("Starting routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().startStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void pauseRoutineStep(StepID stepId,
                                 Consumer<RoutineResponse> onSuccess,
                                 Consumer<RoutineResponse> onFailure) {
        log.debug("Pausing routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().suspendStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void previousRoutineStep(StepID stepId,
                                    Consumer<RoutineResponse> onSuccess,
                                    Consumer<RoutineResponse> onFailure) {
        log.debug("Going to previous step of routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().previousStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void completeRoutineStep(StepID stepId,
                                    Consumer<RoutineResponse> onSuccess,
                                    Consumer<RoutineResponse> onFailure) {
        log.debug("Completing routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().completeStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void skipRoutineStep(StepID stepId,
                                Consumer<RoutineResponse> onSuccess,
                                Consumer<RoutineResponse> onFailure) {
        log.debug("Skipping routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().skipStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void skipRoutine(RoutineID routineId,
                            Consumer<RoutineResponse> onSuccess,
                            Consumer<RoutineResponse> onFailure) {
        log.debug("Skipping routine: " + routineId);
        tryRoutineServiceCall(() -> services.routine().skipRoutine(routineId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void postponeRoutineStep(StepID stepID,
                                    Consumer<RoutineResponse> onSuccess,
                                    Consumer<RoutineResponse> onFailure) {
        log.debug("Postponing routine step: " + stepID);
        tryRoutineServiceCall(() -> services.routine().postponeStep(stepID, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    public void excludeStep(StepID stepID,
                            Consumer<RoutineResponse> onSuccess,
                            Consumer<RoutineResponse> onFailure) {
        log.debug("Postponing routine step: " + stepID);
        tryRoutineServiceCall(() -> services.routine().excludeStep(stepID, LocalDateTime.now()),
                onSuccess, onFailure);
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
    public void setRoutineStepExcluded(StepID stepId, boolean exclude,
                                       Consumer<RoutineResponse> onSuccess,
                                       Consumer<RoutineResponse> onFailure) {
        log.debug("Setting routine step excluded: " + stepId + " as " + exclude);
        tryRoutineServiceCall(() -> services.routine().setStepExcluded(stepId, LocalDateTime.now(), exclude),
                onSuccess, onFailure);
    }


    //==================================================================================================================
    // Routine Broadcaster
    //==================================================================================================================

    @Getter (AccessLevel.NONE)
    private final MapBroadcaster<RoutineID, Routine> routineCardBroadcaster = new MapBroadcaster<>();

    @Getter (AccessLevel.NONE)
    private final Set<RoutineID> trackedRoutines = new HashSet<>();

    public void registerRoutineCard(RoutineID routineId, Consumer<Routine> listener) {
        if (!trackedRoutines.contains(routineId)) {
            services.routine().register(routineId, r -> routineCardBroadcaster.broadcast(routineId, r));
            trackedRoutines.add(routineId);
        }

        routineCardBroadcaster.register(routineId, r -> {
            uiAccessor.acquire(() -> listener.accept(r));
        });
    }
}