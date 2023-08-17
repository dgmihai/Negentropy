package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SpringComponent
@VaadinSessionScope
@Accessors(fluent = true)
@Getter
@Slf4j
public class ClientDataControllerImpl implements ClientDataController {
    private TaskNodeProvider activeTaskNodeProvider;

    @Autowired protected TaskNetworkGraph taskNetworkGraph;
    @Autowired protected TaskEntryDataProviderManager taskEntryDataProviderManager;

    @Autowired protected SessionServices services;
    @Autowired protected UserSettings settings;

    @Autowired private RoutineDataProvider routineDataProvider;

    private <T extends SyncResponse> T tryRequest(Supplier<T> serviceCall) throws Exception {
        T response = serviceCall.get();
        if (!response.success()) {
            NotificationError.show(response.message());
        }
        this.sync(response);
        return response;
    }

    private DataMapResponse tryDataRequest(Supplier<DataMapResponse> serviceCall) {
        try {
            return tryRequest(serviceCall);
        } catch (Throwable t) {
            NotificationError.show(t);
            return new DataMapResponse(t.getMessage());
        }
    }

    private SyncResponse trySyncRequest(Supplier<SyncResponse> serviceCall) {
        try {
            return tryRequest(serviceCall);
        } catch (Throwable t) {
            NotificationError.show(t);
            return new SyncResponse(t.getMessage());
        }
    }

    @Override
    public DataMapResponse requestChange(Change change) {
        return this.requestChanges(List.of(change));
    }

    @Override
    public DataMapResponse requestChanges(List<Change> changes) {
        return tryDataRequest(() -> services.change().execute(Request.of(taskNetworkGraph.syncId(), changes)));
    }

    private synchronized void sync() {
        try {
            this.sync(trySyncRequest(() -> services.query().sync(taskNetworkGraph.syncId())));
        } catch (Throwable t) {
            NotificationError.show(t);
        }
    }

    private synchronized void sync(SyncResponse syncResponse) {
        SyncRecord aggregateSyncRecord = syncResponse.aggregateSyncRecord();
        List<Change> changes = syncResponse.aggregateSyncRecord().changes();
        log.info("Syncing with sync id {}, {} changes, current sync id: {}",
                (aggregateSyncRecord != null ? aggregateSyncRecord.id() : null),
                changes.size(), taskNetworkGraph.syncId());

        Map<TaskID, Task> taskMap = taskNetworkGraph.taskMap();
        Map<LinkID, TaskNode> nodeMap = taskNetworkGraph.nodeMap();
        MultiValueMap<TaskID, LinkID> nodesByTaskMap = taskNetworkGraph.nodesByTaskMap();

        for (Change change : changes) {
            if (change instanceof Change.MergeChange<?> mergeChange) {
                Object mergeData = mergeChange.data();
                if (mergeData instanceof Task task) {
                    log.debug("Got merged task {}", task);
                    taskMap.put(task.id(), task);
                    taskEntryDataProviderManager.pendingTaskRefresh().put(task.id(), false);
                } else if (mergeData instanceof TaskNode node) {
                    log.debug("Got merged node {}", node);
                    nodeMap.put(node.id(), node);
                    taskMap.put(node.task().id(), node.task());
                    taskEntryDataProviderManager.pendingNodeRefresh().put(node.id(), false);
                } else if (mergeData instanceof Tag) {
                    // TODO: No op for now
                } else {
                    throw new IllegalStateException("Unexpected value: " + mergeChange.data());
                }
            } else if (change instanceof Change.PersistChange<?> persist) {
                Object persistData = persist.data();
                if (persistData instanceof Task task) {
                    log.debug("Got persisted task {}", task);
                    taskNetworkGraph.addTask(task);
                } else if (persistData instanceof TaskNode node) {
                    log.debug("Got persisted node {}", node);
                    taskNetworkGraph.addTaskNode(node);
                    taskEntryDataProviderManager.pendingNodeRefresh().put(node.id(), true);
                } else if (persistData instanceof Tag) {
                    // TODO: No op for now
                } else {
                    throw new IllegalStateException("Unexpected value: " + persist.data());
                }
            } else if (change instanceof Change.DeleteChange<?> deleteChange) {
                Object deleteData = deleteChange.data();
                if (deleteData instanceof TaskID taskId) {
                    log.debug("Got deleted task {}", taskMap.get(taskId));
                    taskNetworkGraph.removeTask(taskId);
                } else if (deleteData instanceof LinkID linkId) {
                    log.debug("Got deleted node {}", nodeMap.get(linkId));
                    TaskNode deletedNode = nodeMap.getOrDefault(linkId, null);
                    if (deletedNode == null) {
                        log.warn("Deleted node {} not found in node map, may have already been processed earlier", linkId);
                    } else {
                        taskEntryDataProviderManager.pendingNodeRefresh().put(linkId, true);
                    }
                    taskNetworkGraph.removeTaskNode(linkId);
                } else if (deleteData instanceof TagID) {
                    // TODO: No op for now
                } else {
                    throw new IllegalStateException("Unexpected value: " + deleteChange.data());
                }
            } else {
                log.warn("Unknown change type: {}", change);
            }
        }

        if (aggregateSyncRecord != null) {
            taskNetworkGraph.syncId(aggregateSyncRecord.id());
        }
        taskEntryDataProviderManager.refreshQueuedItems();
//        taskEntryDataProviderManager.refreshAllProviders();
//        taskEntryDataProviderManager.resetAllData();
    }

    @Override
    public void activeTaskNodeProvider(TaskNodeProvider activeTaskProvider) {
        this.activeTaskNodeProvider = activeTaskProvider;
    }

    @Override
    public void recalculateTimeEstimates() {
        log.debug("Recalculating time estimates");
        // NO-OP
//        changeService.recalculateTimeEstimates();
//        taskNetworkGraph.sync();
    }

    //==================================================================================================================
    // Routine Management
    //==================================================================================================================

    private RoutineResponse tryRoutineServiceCall(Supplier<RoutineResponse> serviceCall) {
        try {
            RoutineResponse response = serviceCall.get();

            if (!response.success()) {
                NotificationError.show(response.message());
            }

            return response;
        } catch (Throwable t) {
            NotificationError.show(t);
            return new RoutineResponse(false, null, t.getMessage());
        }
    }

    @Override
    public RoutineResponse createRoutine(TaskID taskId) {
        log.debug("Creating routine from task: " + taskId);
        RoutineResponse response = tryRoutineServiceCall(() -> services.routine().createRoutine(taskId));
        routineDataProvider.refreshAll();
        return response;
    }

    private RoutineResponse processStep(
            Supplier<RoutineResponse> serviceCall) {
        return tryRoutineServiceCall(serviceCall);
    }

    @Override
    public RoutineResponse startRoutineStep(StepID stepId) {
        log.debug("Starting routine step: " + stepId);
        RoutineResponse response = this.processStep(
                () -> services.routine().startStep(stepId, LocalDateTime.now()));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse pauseRoutineStep(StepID stepId) {
        log.debug("Pausing routine step: " + stepId);
        RoutineResponse response = this.processStep(
                () -> services.routine().suspendStep(stepId, LocalDateTime.now()));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse previousRoutineStep(StepID stepId) {
        log.debug("Going to previous step of routine step: " + stepId);
        RoutineResponse response = this.processStep(
                () -> services.routine().previousStep(stepId, LocalDateTime.now()));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse completeRoutineStep(StepID stepId) {
        log.debug("Completing routine step: " + stepId);
        RoutineResponse response = this.processStep(
                () -> services.routine().completeStep(stepId, LocalDateTime.now()));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse skipRoutineStep(StepID stepId) {
        log.debug("Skipping routine step: " + stepId);
        RoutineResponse response = this.processStep(
                () -> services.routine().skipStep(stepId, LocalDateTime.now()));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse skipRoutine(RoutineID routineId) {
        log.debug("Skipping routine: " + routineId);
        RoutineResponse response = this.processStep(
                () -> services.routine().skipRoutine(routineId, LocalDateTime.now()));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse moveRoutineStep(InsertLocation insertLocation, RoutineStep step, RoutineStep target) {
        log.debug("Moving routine step: " + step + " " + insertLocation + " " + target);

        StepID parentId = target.parentId();

        int targetPosition;
        if (parentId != null) {
            RoutineStep parentStep = services.routine().fetchRoutineStep(target.parentId());
            targetPosition =  parentStep.children().indexOf(target);
        } else {
            Routine routine = services.routine().fetchRoutine(target.routineId());
            targetPosition = routine.steps().indexOf(target);
        }

        int position = switch(insertLocation) {
            case BEFORE -> targetPosition - 1;
            case AFTER -> targetPosition + 1;
            default -> throw new RuntimeException("Invalid insert location specified.");
        };

        RoutineResponse response = this.processStep(
                () -> services.routine().moveStep(step.id(), parentId, position));
        routineDataProvider.refreshAll();
        return response;
    }

    @Override
    public RoutineResponse setRoutineStepExcluded(StepID stepId, boolean exclude) {
        log.debug("Setting routine step excluded: " + stepId + " as " + exclude);
        RoutineResponse response =  this.processStep(
                () -> services.routine().setStepExcluded(stepId, LocalDateTime.now(), exclude));
        routineDataProvider.refreshAll();
        return response;
    }
}
