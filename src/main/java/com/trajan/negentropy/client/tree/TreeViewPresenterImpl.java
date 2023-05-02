package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.tree.data.TaskEntryDataProvider;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.facade.TagService;
import com.trajan.negentropy.server.facade.TaskQueryService;
import com.trajan.negentropy.server.facade.TaskUpdateService;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskID;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Function;

@UIScope
@SpringComponent
@Accessors(fluent = true)
@Getter
public class TreeViewPresenterImpl implements TreeViewPresenter {
    private static final Logger logger = LoggerFactory.getLogger(TreeViewPresenterImpl.class);

    private TreeView view;
    private TaskForm form;
    private TreeGridLayout gridLayout;
    private TaskEntryDataProvider dataProvider;

    @Autowired private TaskQueryService queryService;
    @Autowired private TaskUpdateService updateService;
    @Autowired private TagService tagService;

    @Override
    public void initTreeView(TreeView treeView) {
        this.view = treeView;
        this.form = view.form();
        this.gridLayout = view.gridLayout();

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

    private <T> void process(
            T input,
            Function<T, Response> updateFunction,
            boolean fullRefresh) {

        Response response = updateFunction.apply(input);
        if (!response.success()) {
            NotificationError.show(response.message());
        }

        if (!fullRefresh && input instanceof TaskEntry entry) {
            dataProvider.refreshItem(entry);
        } else {
            dataProvider.refreshAll();
        }
    }

    private <T> Response processSequentially(
            T input,
            List<Function<T, Response>> updateFunctions,
            boolean fullRefresh) {

        Response response = new Response(false, "Function list was empty.");

        for (Function<T, Response> updateFunction : updateFunctions) {
            response = updateFunction.apply(input);
            if (!response.success()) {
                NotificationError.show(response.message());
                break;
            }
        }

        if (fullRefresh && input instanceof TaskEntry entry) {
            this.loadData();
            dataProvider.refreshItem(entry);
        } else {
            this.loadData();
        }
        return null;
    }

    @Override
    public void updateNode(TaskEntry entry) {
        this.process(entry, e -> updateService.updateNode(entry.node()), false);
    }

    @Override
    public void deleteNode(TaskEntry entry) {
        process(entry, e -> updateService.deleteNode(e.node().linkId()), true);
    }

    @Override
    public void updateTask(TaskEntry entry) {
        this.process(entry, e -> updateService.updateTask(entry.task()), false);
    }

    @Override
    public void updateEntry(TaskEntry entry) {
        processSequentially(entry, List.of(
                e -> updateService.updateNode(e.node()),
                e -> updateService.updateTask(e.task())),
                false);
    }

    @Override
    public void moveNodeToRoot(TaskEntry entry) {
        processSequentially(entry, List.of(
                e -> updateService.insertTaskAsRoot(e.task().id()),
                e -> updateService.deleteNode(e.node().linkId())),
                true);
    }

    @Override
    public void moveNodeInto(TaskEntry moved, TaskEntry target) {
        processSequentially(moved, List.of(
                m -> updateService.insertTaskAsChild(target.task().id(), m.task().id()),
                m -> updateService.deleteNode(m.node().linkId())),
                true);
    }

    @Override
    public void moveNodeBefore(TaskEntry moved, TaskEntry target) {
        processSequentially(moved, List.of(
                        m -> updateService.insertTaskAsChildAt(
                                target.node().position(),
                                target.task().id(),
                                m.task().id()),
                        m -> updateService.deleteNode(m.node().linkId())),
                true);
    }

    @Override
    public void moveNodeAfter(TaskEntry moved, TaskEntry target) {
        processSequentially(moved, List.of(
                        m -> updateService.insertTaskAsChildAt(
                                target.node().position() + 1,
                                target.task().id(),
                                m.task().id()),
                        m -> updateService.deleteNode(m.node().linkId())),
                true);
    }

    @Override
    public void addTaskFromFormAsChild(TaskEntry parent) {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            process(task,
                    t -> updateService.insertTaskAsChild(
                            parent.task().id(),
                            t.id()),
                    true);
        }
    }

    @Override
    public void addTaskFromFormBefore(TaskEntry after) {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            process(task,
                    t -> updateService.insertTaskAsChildAt(
                            after.node().position(),
                            after.node().parentId(),
                            task.id()),
                    true);
        }
    }

    @Override
    public void addTaskFromFormAfter(TaskEntry before) {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            process(task,
                    t -> updateService.insertTaskAsChildAt(
                            before.node().position() + 1,
                            before.node().parentId(),
                            task.id()),
                    true);
        }
    }

    @Override
    public boolean isTaskFormValid() {
        return form.binder().isValid();
    }

    @Override
    public TagEntity createTag(TagEntity tag) {
        //form.tagBox.setItems(tagService.findAll());
        return tagService.create(tag);
    }

    @Override
    public void onTaskFormSave() {
        if (form.binder().isValid()) {
            Task task = form.binder().getBean();
            TaskID parentId = dataProvider.getBaseEntry() == null ?
                    null :
                    dataProvider.getBaseEntry().node().childId();
            if (task.id() != null) {
                processSequentially(
                        task,
                        List.of(
                                t -> updateService.updateTask(t),
                                t -> updateService.insertTaskAsChild(parentId, t.id())),
                        true);
            } else {
                // TODO: Sequential processing carries through task if response contains it
                TaskResponse response = updateService.createTask(task);
                if (response.success()) {
                    task = response.task();
                    process(
                            task,
                            t -> updateService.insertTaskAsChild(parentId, t.id()),
                            true);
                }
            }
        }
    }
}
