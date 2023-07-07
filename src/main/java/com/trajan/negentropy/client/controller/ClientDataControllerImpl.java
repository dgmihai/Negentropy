package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.data.RoutineDataProvider;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.client.controller.data.TaskEntryDataProvider;
import com.trajan.negentropy.client.controller.data.TaskProvider;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.UpdateService;
import com.trajan.negentropy.server.facade.model.*;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.server.facade.response.TagResponse;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SpringComponent
@VaadinSessionScope
@Accessors(fluent = true)
@Getter
public class ClientDataControllerImpl implements ClientDataController {
    private static final Logger logger = LoggerFactory.getLogger(ClientDataControllerImpl.class);
    
    private TaskProvider activeTaskProvider;

    @Autowired private TaskEntryDataProvider dataProvider;
    @Autowired private QueryService queryService;
    @Autowired private UpdateService updateService;
    @Autowired private RoutineService routineService;
    @Autowired private SessionSettings settings;

    @Autowired private RoutineDataProvider routineCache;

    @Override
    public TaskEntry getBaseEntry() {
        return dataProvider.getBaseEntry();
    }

    @Override
    public void setBaseEntry(TaskEntry entry) {
        dataProvider.setBaseEntry(entry);
    }

    private <T extends Response> T tryServiceCall(Supplier<T> serviceCall) {
        T response = serviceCall.get();

        if (!response.success()) {
            NotificationError.show(response.message());
        }

        return response;
    }

    @SafeVarargs
    private Response tryServiceCalls(Supplier<Response>... serviceCalls) {
        Response response = new Response(false, "Call list was empty.");

        for (Supplier<Response> serviceCall : serviceCalls) {
            response = tryServiceCall(serviceCall);
            if (!response.success()) {
                break;
            }
        }

        return response;
    }

    @Override
    public void updateNode(TaskEntry entry) {
        logger.debug("Updating node: " + entry);
        this.tryServiceCall(
                () -> updateService.updateNode(entry.node()));
        dataProvider.refreshMatchingItems(entry.node().child().id(), true);
    }

    @Override
    public void updateNode(TaskNode node) {
        logger.debug("Updating node: " + node);
        this.tryServiceCall(
                () -> updateService.updateNode(node));
        dataProvider.refreshAll();
        // TODO: Do we need to refresh matching tasks by node?
    }

    @Override
    public void deleteNode(TaskEntry entry) {
        logger.debug("Deleting node: " + entry);
        this.tryServiceCall(
                () -> updateService.deleteNode(entry.node().linkId()));
        dataProvider.refreshAll();
    }

    @Override
    public TaskResponse updateTask(Task task) {
        logger.debug("Updating task: " + task);
        TaskResponse response = this.tryServiceCall(
                () -> updateService.updateTask(task));
        dataProvider.refreshMatchingItems(task.id(), true);
        return response;
    }

    @Override
    public TaskResponse updateTask(TaskEntry entry) {
        return this.updateTask(entry.node().child());
    }

    @Override
    public void updateEntry(TaskEntry entry) {
        logger.debug("Updating entry: " + entry);
        this.tryServiceCalls(
                () -> updateService.updateNode(entry.node()),
                () -> updateService.updateTask(entry.node().child()));
        dataProvider.refreshAll();
    }

    @Override
    public void moveNodeToRoot(TaskEntry entry) {
        logger.debug("Moving to root: " + entry);
        this.tryServiceCalls(
                () -> updateService.insertTaskNode(
                        (TaskNodeDTO) new TaskNodeDTO(entry.node())
                                .parentId(null)
                                .position(null)),
                () -> updateService.deleteNode(entry.node().linkId()));
        dataProvider.refreshAll();
    }

    @Override
    public void moveNodeInto(TaskEntry moved, TaskEntry target) {
        logger.debug("Moving node: " + moved + " into " + target);
        this.tryServiceCalls(
                () -> updateService.insertTaskNode(
                        (TaskNodeDTO) new TaskNodeDTO(moved.node())
                                .parentId(target.node().child().id())
                                .position(null)),
                () -> updateService.deleteNode(moved.node().linkId()));
        dataProvider.refreshAll();
    }

    @Override
    public void moveNodeBefore(TaskEntry moved, TaskEntry target) {
        logger.debug("Moving node: " + moved + " before " + target);
        this.tryServiceCalls(
                () -> updateService.insertTaskNode(
                        (TaskNodeDTO) new TaskNodeDTO(moved.node())
                                .parentId(target.node().parentId())
                                .position(target.node().position())),
                () -> updateService.deleteNode(moved.node().linkId()));
        dataProvider.refreshAll();
    }

    @Override
    public void moveNodeAfter(TaskEntry moved, TaskEntry target) {
        logger.debug("Moving node: " + moved + " after " + target);
        this.tryServiceCalls(
                () -> updateService.insertTaskNode(
                        (TaskNodeDTO) new TaskNodeDTO(moved.node())
                                .parentId(target.node().parentId())
                                .position(target.node().position() + 1)),
                () -> updateService.deleteNode(moved.node().linkId()));
        dataProvider.refreshAll();
    }

    @Override
    public void activeTaskProvider(TaskProvider activeTaskProvider) {
        this.activeTaskProvider = activeTaskProvider;
    }

    @Override
    public Response addTaskFromProvider(TaskProvider taskProvider) {
        TaskEntry parent = dataProvider.getBaseEntry();
        return this.addTaskFromProviderAsChild(taskProvider, parent);
    }

    private Response addTaskDTO(TaskProvider taskProvider, TaskNodeDTO taskDTO) {
        Response validTaskResponse = taskProvider.hasValidTask();
        if (validTaskResponse.success()) {
            try {
                Task task = taskProvider.getTask().orElseThrow();
                this.processTags(task);

                TaskResponse response = this.tryServiceCall(
                        () -> updateService.createTask(task));

                if (response.success()) {
                    this.tryServiceCall(
                            () -> updateService.insertTaskNode(
                                    taskDTO.childId(response.task().id())));
                    dataProvider.refreshAll();
                } else {
                    NotificationError.show(response.message());
                }

                return response;
            } catch (Exception e) {
                NotificationError.show(e);
            }
        } else {
            NotificationError.show(validTaskResponse.message());
        }
        return validTaskResponse;
    }

    private Response tryFunctionWithActiveProvider(Function<TaskProvider, Response> function) {
        if (activeTaskProvider != null) {
            return function.apply(activeTaskProvider);
        } else {
            throw new RuntimeException("No task to add - " +
                    "open either Quick Create, or Create New Task, or start editing a task");
        }
    }

    private Response tryBiFunctionWithActiveProvider(
            BiFunction<TaskProvider, TaskEntry, Response> biFunction, TaskEntry entry) {
        if (activeTaskProvider != null) {
            return biFunction.apply(activeTaskProvider, entry);
        } else {
            throw new RuntimeException("No task to add - " +
                    "open either Quick Create, or Create New Task, or start editing a task");
        }
    }

    @Override
    public Response addTaskFromProviderAsChild(TaskProvider taskProvider, TaskEntry parent) {
        logger.debug("Adding task as a child of " + parent);
        TaskID parentId = parent == null ? null : parent.node().child().id();
        TaskNodeDTO taskNodeDTO = new TaskNodeDTO(
                parentId,
                null,
                taskProvider.getNodeInfo());
        logger.debug("Provided node info: " + taskProvider.getNodeInfo());
        return this.addTaskDTO(taskProvider, taskNodeDTO);
    }

    @Override
    public Response addTaskFromActiveProviderAsChild(TaskEntry parent) {
        return tryBiFunctionWithActiveProvider(this::addTaskFromProviderAsChild, parent);
    }

    @Override
    public Response addTaskFromProviderBefore(TaskProvider taskProvider, TaskEntry next) {
        TaskNodeDTO taskDTO = new TaskNodeDTO(
                next.node().parentId(),
                null,
                taskProvider.getNodeInfo()
                        .position(next.node().position()));
        logger.debug("Adding task " + taskDTO + " before: " + next);
        return this.addTaskDTO(taskProvider, taskDTO);
    }

    @Override
    public Response addTaskFromActiveProviderBefore(TaskEntry after) {
        return tryBiFunctionWithActiveProvider(this::addTaskFromProviderBefore, after);
    }

    @Override
    public Response addTaskFromProviderAfter(TaskProvider taskProvider, TaskEntry prev) {
        TaskNodeDTO taskDTO = new TaskNodeDTO(
                prev.node().parentId(),
                null,
                taskProvider.getNodeInfo()
                        .position(prev.node().position()+1));
        logger.debug("Adding task " + taskDTO + " after: " + prev);
        return this.addTaskDTO(taskProvider, taskDTO);
    }

    @Override
    public Response addTaskFromActiveProviderAfter(TaskEntry before) {
        return tryBiFunctionWithActiveProvider(this::addTaskFromProviderAfter, before);
    }

    @Override
    public Tag createTag(Tag tag) {
        TagResponse response = this.tryServiceCall(
                () -> updateService.createTag(tag));
        return response.tag();
    }

    private void processTags(Task task ) {
        Set<Tag> processedTags = new HashSet<>();

        for (Tag tag : task.tags()) {
            if (tag.id() == null) {
                TagResponse response = this.tryServiceCall(
                        () -> updateService.findTagOrElseCreate(tag.name()));
                if (response.success()) {
                    processedTags.add(response.tag());
                } else {
                    throw new RuntimeException("Failed to process tags in new task");
                }

                processedTags.add(response.tag());
            } else {
                processedTags.add(queryService.fetchTag(tag.id())); // TODO: Filter, list of tags
            }

        }
        task.tags(processedTags);
    }

    @Override
    public void recalculateTimeEstimates() {
        logger.debug("Recalculating time estimates");
        updateService.recalculateTimeEstimates();
        dataProvider.refreshAll();
    }

    // Routine Management

    @Override
    public RoutineResponse createRoutine(TaskID taskId) {
        logger.debug("Creating routine from task: " + taskId);
        return tryServiceCall(() -> routineService.createRoutine(taskId));
    }

    private RoutineResponse processStep(
            Supplier<RoutineResponse> serviceCall) {
        return tryServiceCall(serviceCall);
    }

    @Override
    public RoutineResponse startRoutineStep(StepID stepId) {
        logger.debug("Starting routine step: " + stepId);
        return this.processStep(
                () -> routineService.startStep(stepId, LocalDateTime.now()));
    }

    @Override
    public RoutineResponse pauseRoutineStep(StepID stepId) {
        logger.debug("Pausing routine step: " + stepId);
        return this.processStep(
                () -> routineService.suspendStep(stepId, LocalDateTime.now()));
    }

    @Override
    public RoutineResponse previousRoutineStep(StepID stepId) {
        logger.debug("Going to previous step of routine step: " + stepId);
        return this.processStep(
                () -> routineService.previousStep(stepId, LocalDateTime.now()));
    }

    @Override
    public RoutineResponse completeRoutineStep(StepID stepId) {
        logger.debug("Completing routine step: " + stepId);
        return this.processStep(
                () -> routineService.completeStep(stepId, LocalDateTime.now()));
    }

    @Override
    public RoutineResponse skipRoutineStep(StepID stepId) {
        logger.debug("Skipping routine step: " + stepId);
        return this.processStep(
                () -> routineService.skipStep(stepId, LocalDateTime.now()));
    }

    @Override
    public RoutineResponse skipRoutine(RoutineID routineId) {
        logger.debug("Skipping routine: " + routineId);
        RoutineResponse response = this.processStep(
                () -> routineService.skipRoutine(routineId, LocalDateTime.now()));
        routineCache.refreshAll();
        return response;
    }
}
