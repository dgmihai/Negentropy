package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.tree.data.TaskEntryDataProvider;
import com.trajan.negentropy.client.util.TaskProvider;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.response.Response;

public interface TreeViewPresenter {

    TaskEntry getBaseEntry();
    void setBaseEntry(TaskEntry entry);

    void deleteNode(TaskEntry entry);

    void updateNode(TaskEntry entry);
    void updateTask(Task task);
    void updateTask(TaskEntry entry);
    void updateEntry(TaskEntry entry);

    void moveNodeToRoot(TaskEntry entry);

    void moveNodeInto(TaskEntry moved, TaskEntry target);

    void moveNodeBefore(TaskEntry moved, TaskEntry target);
    void moveNodeAfter(TaskEntry moved, TaskEntry target);

    TaskProvider activeTaskProvider();
    void activeTaskProvider(TaskProvider activeTaskProvider);

    Response addTaskFromProvider(TaskProvider taskProvider, boolean top);
    Response addTaskFromProvider(TaskProvider taskProvider);
    Response addTaskFromActiveProvider();

    Response addTaskFromProviderAsChild(TaskProvider taskProvider, TaskEntry parent, boolean top);

    Response addTaskFromProviderAsChild(TaskProvider taskProvider, TaskEntry parent);
    Response addTaskFromActiveProviderAsChild(TaskEntry parent);

    Response addTaskFromProviderBefore(TaskProvider taskProvider, TaskEntry after);
    Response addTaskFromActiveProviderBefore(TaskEntry after);

    Response addTaskFromProviderAfter(TaskProvider taskProvider, TaskEntry before);
    Response addTaskFromActiveProviderAfter(TaskEntry before);

    Tag createTag(Tag tag);

    QueryService queryService();

    void recalculateTimeEstimates();

    TaskEntryDataProvider dataProvider();

}
