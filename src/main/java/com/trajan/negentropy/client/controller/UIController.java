package com.trajan.negentropy.client.controller;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.controller.util.HasRootNode;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.*;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.backend.util.OrphanTaskCleaner;
import com.trajan.negentropy.server.broadcaster.MapBroadcaster;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringComponent
@UIScope
@Getter
@Benchmark(millisFloor = 10)
public class UIController {
    private final UILogger log = new UILogger();

    @Autowired
    private UI ui;

    @Setter
    private TaskNodeProvider activeTaskNodeProvider;
    @Setter
    private HasRootNode activeTaskNodeDisplay;

    @Autowired
    protected TaskNetworkGraph taskNetworkGraph;
    @Autowired
    protected TaskEntryDataProvider taskEntryDataProvider;

    @Autowired
    protected SessionServices services;
    @Autowired
    protected UserSettings settings;

    @Autowired
    protected RoutineDataProvider routineDataProvider;

    @PostConstruct
    public void init() {
        routineCardBroadcaster.label = "Routine Card Broadcaster: ";

        UI currentUI = UI.getCurrent();
        Integer uiId = currentUI != null ? currentUI.getUIId() : null;
        String address = currentUI != null ? currentUI.getSession().getBrowser().getAddress() : null;
        String browser = currentUI != null ? currentUI.getSession().getBrowser().getBrowserApplication() : null;
        log.info("UI client: " + uiId + " at " + address + " using " + browser);

        this.sync();
    }

    @Deprecated
    @VisibleForTesting
    public DataMapResponse requestChange(Change change) {
        return this.requestChanges(List.of(change));
    }

    @Async
    public void requestChangeAsync(Change change) {
        requestChangesAsync(List.of(change), null);
    }

    @Async
    public void requestChangeAsync(Change change, Consumer<DataMapResponse> callback) {
        this.requestChangesAsync(List.of(change), callback);
    }

    @Deprecated
    @VisibleForTesting
    public synchronized DataMapResponse requestChanges(List<Change> changes) {
        return services.change().execute(Request.of(taskNetworkGraph.syncId(), changes));
    }

    @Async
    public void requestChangesAsync(List<Change> changes) {
        requestChangesAsync(changes, null);
    }

    @Async
    public synchronized void requestChangesAsync(List<Change> changes, Consumer<DataMapResponse> callback) {
        try {
            DataMapResponse response = services.change().execute(Request.of(taskNetworkGraph.syncId(), changes));
            accessUI(() -> handleResponse(response, callback));
        } catch (Throwable t) {
            log.error("Error executing request changes", t);
            accessUI(() -> NotificationMessage.error(t));
        }
    }

    @Async
    public synchronized void sync() {
        log.info("Checking for sync");
        try {
            SyncResponse response = services.query().sync(taskNetworkGraph.syncId());
            accessUI(() -> handleResponse(response, null));
        } catch (Throwable t) {
            log.error("Error executing sync", t);
            accessUI(() -> NotificationMessage.error(t));
        }
    }

    private <T extends Response> void handleResponse(T response, Consumer<T> callback) {
        try {
            if (!response.success()) {
                NotificationMessage.error(response.message());
            } else {
                NotificationMessage.result(response.message());
            }

            if (callback != null) {
                callback.accept(response);
            }
        } catch (Throwable t) {
            log.error("Error handling response", t);
            accessUI(() -> NotificationMessage.error(t));
        }
    }

    //==================================================================================================================
    // Miscellaneous
    //==================================================================================================================

    @Autowired
    private NetDurationHelperManager netDurationHelperManager;

    @Async
    public synchronized void recalculateNetDurations() {
        log.debug("Recalculating time estimates");
        netDurationHelperManager.recalculateTimeEstimates();
    }

    @Autowired
    private OrphanTaskCleaner orphanCleaner;

    @Async
    public synchronized void deleteAllOrphanedTasks() {
        // TODO: Go over steps and delete orphaned steps
        log.debug("Deleting orphan tasks");
        try {
            orphanCleaner.deleteAllOrphanedTasks();
            this.sync();
        } catch (Throwable t) {
            log.error("Exception while deleting orphaned tasks: \n", t);
            NotificationMessage.error(t);
        }
    }

    @Async
    public synchronized void deleteAllCompletedTaskNodes() {
        // TODO: Go over steps and delete orphaned steps
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
            RoutineResponse response = serviceCall.get();

            this.execute(() -> accessUI("Routine service call", () -> {
            accessUI(() -> {
                if (!response.success()) {
                    NotificationMessage.error(response.message());
                    if (onFailure != null) onFailure.accept(response);
                } else {
                    NotificationMessage.result(response.message());
                    routineDataProvider.refreshAll();
                    if (onSuccess != null) onSuccess.accept(response);
                }
            });
        } catch (Throwable t) {
            log.error("Exception while executing routine call: \n", t);
            accessUI(() -> NotificationMessage.error(t));
        }
    }

    @Async
    public synchronized void createRoutine(List<PersistedDataDO> rootData, TaskNodeTreeFilter filter,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        if (rootData.isEmpty()) {
            NotificationMessage.error("No root data provided");
            return;
        }

        log.debug("Creating routine from task: " + rootData.get(0).name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootData.stream()
                        .map(data -> (TaskOrLinkID) data.id())
                        .toList(), filter, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Deprecated
    public void createRoutine(Task rootTask,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        log.debug("Creating routine from task: " + rootTask.name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootTask.id(), LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void createRoutine(Task rootTask, TaskNodeTreeFilter filter,
                              Consumer<RoutineResponse> onSuccess,
                              Consumer<RoutineResponse> onFailure) {
        log.debug("Creating routine from task: " + rootTask.name());
        tryRoutineServiceCall(() -> services.routine().createRoutine(rootTask.id(), filter, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void startRoutineStep(StepID stepId,
                                              Consumer<RoutineResponse> onSuccess,
                                              Consumer<RoutineResponse> onFailure) {
        log.debug("Starting routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().startStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void pauseRoutineStep(StepID stepId,
                                 Consumer<RoutineResponse> onSuccess,
                                 Consumer<RoutineResponse> onFailure) {
        log.debug("Pausing routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().suspendStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void previousRoutineStep(StepID stepId,
                                    Consumer<RoutineResponse> onSuccess,
                                    Consumer<RoutineResponse> onFailure) {
        log.debug("Going to previous step of routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().previousStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void completeRoutineStep(StepID stepId,
                                    Consumer<RoutineResponse> onSuccess,
                                    Consumer<RoutineResponse> onFailure) {
        log.debug("Completing routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().completeStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void skipRoutineStep(StepID stepId,
                                Consumer<RoutineResponse> onSuccess,
                                Consumer<RoutineResponse> onFailure) {
        log.debug("Skipping routine step: " + stepId);
        tryRoutineServiceCall(() -> services.routine().skipStep(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void postponeRoutineStep(StepID stepID,
                                    Consumer<RoutineResponse> onSuccess,
                                    Consumer<RoutineResponse> onFailure) {
        log.debug("Postponing routine step: " + stepID);
        tryRoutineServiceCall(() -> services.routine().postponeStep(stepID, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void excludeRoutineStep(StepID stepID,
                                   Consumer<RoutineResponse> onSuccess,
                                   Consumer<RoutineResponse> onFailure) {
        log.debug("Postponing routine step: " + stepID);
        tryRoutineServiceCall(() -> services.routine().excludeStep(stepID, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    //@Override
    @Async
    public synchronized RoutineResponse moveRoutineStep(InsertLocation insertLocation, RoutineStep step, RoutineStep target) {
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

    @Async
    public synchronized void setRoutineStepExcluded(StepID stepId, boolean exclude,
                                       Consumer<RoutineResponse> onSuccess,
                                       Consumer<RoutineResponse> onFailure) {
        log.debug("Setting routine step excluded: " + stepId + " as " + exclude);
        tryRoutineServiceCall(() -> services.routine().setStepExcluded(stepId, LocalDateTime.now(), exclude),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void kickUpStep(StepID stepId,
                           Consumer<RoutineResponse> onSuccess,
                           Consumer<RoutineResponse> onFailure) {
        log.debug("Kicking routine step up one level: " + stepId);
        tryRoutineServiceCall(() -> services.routine().kickStepUp(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    @Async
    public synchronized void pushStepForward(StepID stepId,
                           Consumer<RoutineResponse> onSuccess,
                           Consumer<RoutineResponse> onFailure) {
        log.debug("Pushing routine step forward by one level: " + stepId);
        tryRoutineServiceCall(() -> services.routine().pushStepForward(stepId, LocalDateTime.now()),
                onSuccess, onFailure);
    }

    //==================================================================================================================
    // Routine Broadcaster
    //==================================================================================================================

    @Getter (AccessLevel.NONE)
    @Autowired private MapBroadcaster<RoutineID, Routine> routineCardBroadcaster;

    @Getter (AccessLevel.NONE)
    private final Set<RoutineID> trackedRoutines = new HashSet<>();

    @Getter
    private final Set<Task> tasksWithActiveSteps = new HashSet<>();

    public Registration registerRoutineCard(RoutineID routineId, Consumer<Routine> listener) {
        log.debug("Registering routine card");
        if (!trackedRoutines.contains(routineId)) {
            services.routine().register(routineId, r -> routineCardBroadcaster.broadcast(routineId, r));
            trackedRoutines.add(routineId);
        } else {
            log.warn("Routine already tracked: " + routineId);
        }

        return routineCardBroadcaster.register(routineId, r -> {
            log.debug("Routine card update for <" + r.name() + ">");
            accessUI(() -> listener.accept(r));
        });
    }

    //==================================================================================================================
    // UI Accessor
    //==================================================================================================================

    public void accessUI(Command command) {
        if (ui != null) {
            ui.access(command);
        } else {
            log.warn("UI instance not available");
            command.execute();
        }
    }
}