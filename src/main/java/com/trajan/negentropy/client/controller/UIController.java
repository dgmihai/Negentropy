package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.components.grid.TaskTreeGrid;
import com.trajan.negentropy.client.controller.dataproviders.RoutineDataProvider;
import com.trajan.negentropy.client.controller.dataproviders.TaskEntryDataProviderManager;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeDisplay;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.sessionlogger.SessionLogged;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;
import com.trajan.negentropy.client.sessionlogger.SessionLoggerFactory;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.Broadcaster;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.backend.util.OrphanTaskCleaner;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    @Setter private TaskNodeDisplay activeTaskNodeDisplay;

    @Autowired protected TaskNetworkGraph taskNetworkGraph;
    @Autowired protected TaskEntryDataProviderManager taskEntryDataProviderManager;

    @Autowired protected SessionServices services;
    @Autowired protected UserSettings settings;

    @Autowired private RoutineDataProvider routineDataProvider;

    @Autowired private Broadcaster serverBroadcaster;

    private Registration broadcastRegistration;

    @PostConstruct
    public void init() {
        log = getLogger(this.getClass());

        Integer uiId = currentUI != null ? currentUI.getUIId() : null;
        String address = currentUI != null ? currentUI.getSession().getBrowser().getAddress() : null;
        String browser = currentUI != null ? currentUI.getSession().getBrowser().getBrowserApplication() : null;
        log.info("Registering UI client " + uiId + " at " + address + " using " + browser +
                " with server broadcaster");
        broadcastRegistration = serverBroadcaster.register(this::sync);

        this.sync();
    }

    @PreDestroy
    public void destroy() {
        broadcastRegistration.remove();
        broadcastRegistration = null;
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

                this.sync(currentUI, response.aggregateSyncRecord());
            });
        } else {
            this.sync(response.aggregateSyncRecord());
        }
        return response;
    }

    private DataMapResponse tryDataRequest(Supplier<DataMapResponse> serviceCall) {
        try {
            return tryRequest(serviceCall);
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
    public DataMapResponse requestChange(Change change) {
        return this.requestChanges(List.of(change));
    }

    public void requestChangeAsync(Change change) {
        requestChangeAsync(change, null);
    }

    public void requestChangeAsync(Change change, TaskTreeGrid<?> gridRequiringFilterRefresh) {
        CompletableFuture.runAsync(() -> {
                this.requestChanges(List.of(change));
                if (gridRequiringFilterRefresh instanceof TaskEntryTreeGrid entryTreeGrid) {
                    entryTreeGrid.gridDataProvider().refreshFilter();
                }
        });
    }

    //@Override
    @Deprecated
    public DataMapResponse requestChanges(List<Change> changes) {
        return this.tryDataRequest(() -> services.change().execute(
                Request.of(taskNetworkGraph.syncId(), changes)));
    }

    public void requestChangesAsync(List<Change> changes) {
        requestChangesAsync(changes, null);
    }

    public void requestChangesAsync(List<Change> changes, TaskTreeGrid<?> gridRequiringFilterRefresh) {
        CompletableFuture.runAsync(() -> {
            this.tryDataRequest(() -> services.change().execute(
                    Request.of(taskNetworkGraph.syncId(), changes)));
            if (gridRequiringFilterRefresh instanceof TaskEntryTreeGrid entryTreeGrid) {
                entryTreeGrid.gridDataProvider().refreshFilter();
            }
        });
    }

    public void sync() {
        log.info("Checking for sync");
        SyncResponse response = trySyncRequest(() -> services.query().sync(taskNetworkGraph.syncId()));
        currentUI.access(() -> {
            try {
                this.sync(currentUI, response.aggregateSyncRecord());
            } catch (Throwable t) {
                NotificationMessage.error(t);
            }
        });
    }

    private void sync(SyncRecord syncRecord) {
        if (currentUI == null) {
            this.sync(null, syncRecord);
        } else {
            currentUI.access(() -> this.sync(currentUI, syncRecord));
        }
    }

    private synchronized void sync(UI currentUI, SyncRecord aggregateSyncRecord) {
        if (taskNetworkGraph.syncId() != aggregateSyncRecord.id()) {
            List<Change> changes = aggregateSyncRecord.changes();
            log.info("Syncing with sync id {}, {} changes, current sync id: {}",
                    aggregateSyncRecord.id(), changes.size(), taskNetworkGraph.syncId());

            if (settings != null) {
                taskNetworkGraph.syncNetDurations(settings.filter());
            } else {
                taskNetworkGraph.syncNetDurations(null);
            }

            Map<TaskID, Task> taskMap = taskNetworkGraph.taskMap();
            Map<LinkID, TaskNode> nodeMap = taskNetworkGraph.nodeMap();
            boolean refreshAll = false;

            for (Change change : changes) {
                if (change instanceof MergeChange<?> mergeChange) {
                    Object mergeData = mergeChange.data();
                    if (mergeData instanceof Task task) {
                        log.debug("Got merged task {}", task);
                        taskMap.put(task.id(), task);
                        for (LinkID linkId : taskNetworkGraph.nodesByTaskMap().getOrDefault(task.id(), List.of())) {
                            taskEntryDataProviderManager.pendingNodeRefresh().put(linkId, false);
                        }
                    } else if (mergeData instanceof TaskNode node) {
                        log.debug("Got merged node {}", node);
                        nodeMap.put(node.id(), node);
                        taskMap.put(node.task().id(), node.task());
                        taskEntryDataProviderManager.pendingNodeRefresh().put(node.id(), false);
                    } else if (mergeData instanceof Tag) {
                        taskNetworkGraph.refreshTags();
                    } else {
                        throw new IllegalStateException("Unexpected value: " + mergeChange.data());
                    }
                } else if (change instanceof PersistChange<?> persist) {
                    Object persistData = persist.data();
                    if (persistData instanceof Task task) {
                        log.debug("Got persisted task {}", task);
                        taskNetworkGraph.addTask(task);
                        for (LinkID linkId : taskNetworkGraph.nodesByTaskMap().getOrDefault(task.id(), List.of())) {
                            taskEntryDataProviderManager.pendingNodeRefresh().put(linkId, false);
                        }
                    } else if (persistData instanceof TaskNode node) {
                        log.debug("Got persisted node {}", node);
                        taskNetworkGraph.addTaskNode(node);
                        taskEntryDataProviderManager.pendingNodeRefresh().put(node.id(), false);
                        TaskID parentId = node.parentId();
                        if (parentId != null) {
                            // TODO: Needs to be fixed
//                        taskEntryDataProviderManager.pendingTaskRefresh().put(parentId, true);
                            refreshAll = true;
                        } else {
                            refreshAll = true;
                        }
                    } else if (persistData instanceof Tag) {
                        taskNetworkGraph.refreshTags();
                    } else {
                        throw new IllegalStateException("Unexpected value: " + persist.data());
                    }
                } else if (change instanceof DeleteChange<?> deleteChange) {
                    Object deleteData = deleteChange.data();
                    if (deleteData instanceof TaskID taskId) {
                        log.debug("Got deleted task {}", taskId);
                        taskNetworkGraph.removeTask(taskId);
                    } else if (deleteData instanceof LinkID linkId) {
                        log.debug("Got deleted node {}", linkId);
                        TaskNode deletedNode = nodeMap.getOrDefault(linkId, null);
                        if (deletedNode == null) {
                            log.warn("Deleted node {} not found in node map", linkId);
                            // Refresh all nodes
                            refreshAll = true;
                        } else {
                            TaskID parentId = deletedNode.parentId();
                            if (parentId != null) {
                                for (LinkID lid : taskNetworkGraph.nodesByTaskMap().getOrDefault(parentId, List.of())) {
                                    taskEntryDataProviderManager.pendingNodeRefresh().put(lid, true);
                                }
                            } else {
                                refreshAll = true;
                            }
                        }
                        taskNetworkGraph.removeTaskNode(linkId);
                    } else if (deleteData instanceof TagID) {
                        taskNetworkGraph.refreshTags();
                    } else {
                        throw new IllegalStateException("Unexpected value: " + deleteChange.data());
                    }
                } else {
                    log.warn("Unknown change type: {}", change);
                }

            }

            taskNetworkGraph.syncId(aggregateSyncRecord.id());

            if (refreshAll) {
                taskEntryDataProviderManager.refreshAllProviders();
            } else {
                taskEntryDataProviderManager.refreshQueuedItems();
            }
        } else {
            log.debug("No sync required, sync ID's match");
        }
    }

    @Autowired private NetDurationHelperManager netDurationHelperManager;
    //@Override
    public void recalculateNetDurations() {
        log.debug("Recalculating time estimates");
        netDurationHelperManager.recalculateTimeEstimates();
        this.sync();
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
