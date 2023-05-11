package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.tree.data.TaskEntryDataProvider;
import com.trajan.negentropy.client.util.NotificationError;
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
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@UIScope
@SpringComponent
@Accessors(fluent = true)
@Getter
public class TreeViewPresenterImpl implements TreeViewPresenter {
    private static final Logger logger = LoggerFactory.getLogger(TreeViewPresenterImpl.class);

    private TreeView view;
    private TaskFormLayout form;
    private TaskTreeGrid gridLayout;
    private TaskEntryDataProvider dataProvider;

    @Autowired private QueryService queryService;
    @Autowired private UpdateService updateService;
    @Autowired private TagService tagService;

    @Override
    public void initTreeView(TreeView treeView) {
        this.view = treeView;
        this.form = view.form();
        this.gridLayout = view.taskTreeGrid();

        loadData();
    }

    private void loadData() {
        logger.debug("Refreshed task nodes.");
        this.dataProvider = new TaskEntryDataProvider(queryService);
        gridLayout.treeGrid().setDataProvider(dataProvider);
    }

    @Override
    public void setBaseEntry(TaskEntry entry) {
        dataProvider.setBaseEntry(entry);
    }

    public TaskEntry getBaseEntry() {
        return dataProvider.getBaseEntry();
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
                Refresh.ANCESTORS,
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
    public void addTaskFromFormAsChild(TaskEntry parent) {
        logger.debug("Creating task as a child of " + parent);
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            TaskResponse response = (TaskResponse) this.process(
                    task,
                    Refresh.NONE,
                    t -> updateService.createTask(t));
            if (response.success()) {
                this.process(
                        response.task(),
                        Refresh.ANCESTORS,
                        t -> updateService.insertTaskNode(
                                new TaskNodeDTO(
                                        parent.node().parentId(),
                                        t.id(),
                                        null,
                                        0)));
            }
        }
    }

    @Override
    public void addTaskFromFormBefore(TaskEntry after) {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            TaskResponse response = (TaskResponse) this.process(
                    task,
                    Refresh.NONE,
                    t -> updateService.createTask(t));
            if (response.success()) {
                this.process(
                        response.task(),
                        Refresh.ANCESTORS,
                        t -> updateService.insertTaskNode(
                                new TaskNodeDTO(
                                        after.node().parentId(),
                                        t.id(),
                                        after.node().position(),
                                        null)));
            }
        }
    }

    @Override
    public void addTaskFromFormAfter(TaskEntry before) {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            TaskResponse response = (TaskResponse) this.process(
                    task,
                    Refresh.NONE,
                    t -> updateService.createTask(t));
            if (response.success()) {
                this.process(
                        response.task(),
                        Refresh.ANCESTORS,
                        t -> updateService.insertTaskNode(
                                new TaskNodeDTO(
                                        before.node().parentId(),
                                        t.id(),
                                        before.node().position() + 1,
                                        null)));
            }
        }
    }

    @Override
    public boolean isTaskFormValid() {
        // TODO: Switch between whatever task provider is active
        return form.binder().isValid();
    }

    @Override
    public Tag createTag(Tag tag) {
        TagResponse response = (TagResponse) this.process(
                tag,
                Refresh.NONE,
                t -> updateService.createTag(tag));
        return response.tag();
    }

    @Override
    public void onTaskFormSave(TaskFormLayout form) {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            addTaskAsChildOfBase(task);
        }
    }

    private Response addTaskAsChildOfBase(Task task) {
        TaskID parentId = dataProvider.getBaseEntry() == null ?
                null :
                dataProvider.getBaseEntry().node().childId();
        TaskResponse response = (TaskResponse) this.process(
                task,
                Refresh.NONE,
                t -> updateService.createTask(t));
        if (response.success()) {
            this.process(
                    response.task(),
                    Refresh.ALL,
                    t -> updateService.insertTaskNode(
                            new TaskNodeDTO(
                                    parentId,
                                    t.id(),
                                    null,
                                    null)));
        }
        return response;
    }

    @Override
    public Response onQuickAdd(Task task) {
        Set<Tag> processedTags = new HashSet<>();
        for (Tag tag : task.tags()) {
            if (tag.id() == null) {
                TagResponse response = (TagResponse) this.process(
                        tag,
                        Refresh.NONE,
                        t -> updateService.createTag(tag));
                if (response.success()) {
                    processedTags.add(response.tag());
                } else {
                    return response;
                }
            }

            processedTags.add(queryService.fetchTag(tag.id())); // TODO: Filter
        }
        task.tags(processedTags);
        logger.debug("Quick add : " + task);
        return this.addTaskAsChildOfBase(task);
    }

    @Override
    public void recalculateTimeEstimates() {
        updateService.recalculateTimeEstimates();
        dataProvider.refreshAll();
    }
}
