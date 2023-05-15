package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.tree.data.TaskEntryDataProvider;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TaskProvider;
import com.trajan.negentropy.server.backend.TagService;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.UpdateService;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TagResponse;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@SpringComponent
@VaadinSessionScope
@Accessors(fluent = true)
@Getter
public class TreeViewPresenterImpl implements TreeViewPresenter {
    private static final Logger logger = LoggerFactory.getLogger(TreeViewPresenterImpl.class);

    private TaskEntryDataProvider dataProvider;

    private TaskProvider activeTaskProvider;

    @Autowired private QueryService queryService;
    @Autowired private UpdateService updateService;
    @Autowired private TagService tagService;

    @PostConstruct
    private void init() {
        this.dataProvider = new TaskEntryDataProvider(queryService);
    }

    @Override
    public TaskEntry getBaseEntry() {
        return dataProvider.getBaseEntry();
    }

    @Override
    public void setBaseEntry(TaskEntry entry) {
        dataProvider.setBaseEntry(entry);
    }

    private enum Refresh {
        NONE,
        SINGLE,
        ANCESTORS,
        ALL
    }

    @SafeVarargs
    private <T> Response process(
            T input,
            Refresh refresh,
            Function<T, Response>... updateFunctions) {

        Response response = new Response(false, "Function list was empty.");

        for (Function<T, Response> updateFunction : updateFunctions) {
            response = updateFunction.apply(input);
            if (!response.success()) {
                NotificationError.show(response.message());
                break;
            }
        }

        this.handleRefresh(input, refresh);

        return response;
    }

    private <T> void handleRefresh (T input, Refresh refresh) {
        boolean ancestors = refresh.equals(Refresh.ANCESTORS);
        switch (refresh) {
            case SINGLE, ANCESTORS -> {
                if (input instanceof Task t) {
                    dataProvider.refreshMatchingItems(t.id(), ancestors);
                } else if (input instanceof TaskEntry e) {
                    dataProvider.refreshMatchingItems(e.task().id(), ancestors);
                } else if (input instanceof TaskNode n) {
                    dataProvider.refreshMatchingItems(n.childId(), ancestors);
                } else {
                    logger.warn("Item " + input +
                            " passed to process didn't match Task or TaskEntry or TaskNode");
                }
            }
            case ALL -> dataProvider.refreshAll();
            default -> { }
        }
    }

    @Override
    public void updateNode(TaskEntry entry) {
        logger.debug("Updating node: " + entry);
        this.process(entry,
                Refresh.ANCESTORS,
                e -> updateService.updateNode(entry.node()));
    }

    @Override
    public void deleteNode(TaskEntry entry) {
        logger.debug("Deleting node: " + entry);
        this.process(entry,
                Refresh.ALL,
                e -> updateService.deleteNode(e.node().linkId()));
    }

    @Override
    public void updateTask(Task task) {
        logger.debug("Updating task: " + task);
        this.process(task,
                Refresh.SINGLE,
                e -> updateService.updateTask(task));
    }

    @Override
    public void updateTask(TaskEntry entry) {
        logger.debug("Updating task: " + entry);
        this.process(entry,
                Refresh.ANCESTORS,
                e -> updateService.updateTask(entry.task()));
    }

    @Override
    public void updateEntry(TaskEntry entry) {
        logger.debug("Updating entry: " + entry);
        this.process(entry,
                Refresh.ANCESTORS,
                e -> updateService.updateNode(e.node()),
                e -> updateService.updateTask(e.task()));
    }

    @Override
    public void moveNodeToRoot(TaskEntry entry) {
        logger.debug("Moving to root: " + entry);
        this.process(entry,
                Refresh.ALL,
                e -> updateService.insertTaskNode(
                        new TaskNodeDTO(entry.node())
                                .parentId(null)
                                .position(null)),
                e -> updateService.deleteNode(e.node().linkId()));
    }

    @Override
    public void moveNodeInto(TaskEntry moved, TaskEntry target) {
        logger.debug("Moving node: " + moved + " into " + target);
        this.process(moved,
                Refresh.ALL,
                m -> updateService.insertTaskNode(
                        new TaskNodeDTO(moved.node())
                                .parentId(target.task().id())
                                .position(null)),
                m -> updateService.deleteNode(m.node().linkId()));
    }

    @Override
    public void moveNodeBefore(TaskEntry moved, TaskEntry target) {
        logger.debug("Moving node: " + moved + " before " + target);
        this.process(moved,
                Refresh.ALL,
                m -> updateService.insertTaskNode(
                        new TaskNodeDTO(m.node())
                                .parentId(target.node().parentId())
                                .position(target.node().position())),
                m -> updateService.deleteNode(m.node().linkId()));
    }

    @Override
    public void moveNodeAfter(TaskEntry moved, TaskEntry target) {
        logger.debug("Moving node: " + moved + " after " + target);
        this.process(moved,
                Refresh.ALL,
                m -> updateService.insertTaskNode(
                        new TaskNodeDTO(m.node())
                                .parentId(target.node().parentId())
                                .position(target.node().position() + 1)),
                m -> updateService.deleteNode(m.node().linkId()));
    }

    @Override
    public void activeTaskProvider(TaskProvider activeTaskProvider) {
        this.activeTaskProvider = activeTaskProvider;
    }

    private Response addTaskDTO(TaskProvider taskProvider, TaskNodeDTO taskDTO) {
        Response validTaskResponse = taskProvider.hasValidTask();
        if (validTaskResponse.success()) {
            try {
                Task task = taskProvider.getTask().orElseThrow();
                task = this.processTags(task);

                if (task == null) {
                    throw new RuntimeException("Failed to process tags in new task");
                }

                TaskResponse response = (TaskResponse) this.process(
                        task,
                        Refresh.NONE,
                        t -> updateService.createTask(t));

                if (response.success()) {
                    this.process(
                            response.task(),
                            Refresh.ALL,
                            t -> updateService.insertTaskNode(
                                    taskDTO.childId(t.id())));
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
    public Response addTaskFromProvider(TaskProvider taskProvider, boolean top) {
        TaskEntry parent = dataProvider.getBaseEntry();
        return this.addTaskFromProviderAsChild(taskProvider, parent, top);
    }

    @Override
    public Response addTaskFromProvider(TaskProvider taskProvider) {
        return this.addTaskFromProvider(taskProvider, false);
    }

    @Override
    public Response addTaskFromActiveProvider() {
        return tryFunctionWithActiveProvider(this::addTaskFromProvider);
    }

    @Override
    public Response addTaskFromProviderAsChild(TaskProvider taskProvider, TaskEntry parent, boolean top) {
        logger.debug("Creating task as a child of " + parent);
        Integer position = top ? 0 : null;
        TaskID parentId = parent == null ? null : parent.task().id();
        TaskNodeDTO taskNodeDTO = new TaskNodeDTO(
                parentId,
                null,
                position,
                0);
        return this.addTaskDTO(taskProvider, taskNodeDTO);
    }

    @Override
    public Response addTaskFromProviderAsChild(TaskProvider taskProvider, TaskEntry parent) {
        return this.addTaskFromProviderAsChild(taskProvider, parent, false);
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
                next.node().position(),
                null);
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
                prev.node().position() + 1,
                null);
        return this.addTaskDTO(taskProvider, taskDTO);
    }

    @Override
    public Response addTaskFromActiveProviderAfter(TaskEntry before) {
        return tryBiFunctionWithActiveProvider(this::addTaskFromProviderBefore, before);
    }

    @Override
    public Tag createTag(Tag tag) {
        TagResponse response = (TagResponse) this.process(
                tag,
                Refresh.NONE,
                t -> updateService.createTag(tag));
        return response.tag();
    }

    private Task processTags(Task task ) {
        Set<Tag> processedTags = new HashSet<>();

        for (Tag tag : task.tags()) {
            if (tag.id() == null) {
                TagResponse response = (TagResponse) this.process(
                        tag,
                        Refresh.NONE,
                        t -> updateService.findTagOrElseCreate(tag.name()));
                if (response.success()) {
                    processedTags.add(response.tag());
                } else {
                    return null;
                }

                processedTags.add(response.tag());
            } else {
                processedTags.add(queryService.fetchTag(tag.id())); // TODO: Filter, list of tags
            }

        }
        return task.tags(processedTags);
    }

    @Override
    public void recalculateTimeEstimates() {
        updateService.recalculateTimeEstimates();
        dataProvider.refreshAll();
    }
}
