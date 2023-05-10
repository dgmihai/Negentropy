package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.response.Response;

public interface TreeViewPresenter {
    void initTreeView(TreeView treeView);

    void setBaseEntry(TaskEntry entry);

    TaskEntry getBaseEntry();

    void updateNode(TaskEntry entry);

    void deleteNode(TaskEntry entry);

    void updateTask(TaskEntry entry);

    void updateEntry(TaskEntry entry);

    void moveNodeToRoot(TaskEntry entry);

    void moveNodeInto(TaskEntry moved, TaskEntry target);

    void moveNodeBefore(TaskEntry moved, TaskEntry target);

    void moveNodeAfter(TaskEntry moved, TaskEntry target);

    void addTaskFromFormAsChild(TaskEntry parent);

    void addTaskFromFormBefore(TaskEntry after);

    void addTaskFromFormAfter(TaskEntry before);

    boolean isTaskFormValid();

    Tag createTag(Tag tag);

    void onTaskFormSave(TaskFormLayout form);

    QueryService queryService();

    Response onQuickAdd(Task task);
}
