package com.trajan.negentropy.client.presenter;

import com.trajan.negentropy.client.TaskEntry;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.service.TaskService;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntryDataProvider.class);

    private final TaskService taskService;

    @Getter
    @Setter
    private Task rootTask = null;

    public TaskEntryDataProvider(TaskService taskService, Task rootTask) {
        this.taskService = taskService;
        this.rootTask = rootTask;
    }

    @Override
    public int getChildCount(HierarchicalQuery<TaskEntry, Void> query) {
        if (query.getParent() == null) {
            if (rootTask == null) return taskService.countOrphanNodes();
            else return taskService.countChildNodes(rootTask.getId());
        }
        return taskService.countChildNodes(query.getParent().node().getReferenceTask().getId());
    }


    @Override
    public boolean hasChildren(TaskEntry taskEntry) {
        return taskService.countChildNodes(taskEntry.node().getReferenceTask().getId()) > 0;
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
        int offset = query.getOffset();
        int limit = query.getLimit();
        TaskEntry parent = query.getParent();
        Stream<TaskNode> nodeStream;
        if (parent == null) nodeStream = (rootTask == null) ?
                taskService.getOrphanNodes().stream() :
                taskService.getChildNodes(rootTask.getId()).stream();
        else nodeStream = taskService.getChildNodes(parent.node().getReferenceTask().getId()).stream();
        return nodeStream
                .skip(offset)
                .limit(limit)
                .map(node -> new TaskEntry(node, parent));
    }

    @Override
    public void refreshItem(TaskEntry item, boolean updateChildren) {
        refreshItem(item, updateChildren, new HashSet<>());
    }

    private void refreshItem(TaskEntry item, boolean updateChildren, Set<TaskEntry> refreshedItems) {
        if (!refreshedItems.contains(item)) {
            super.refreshItem(item, updateChildren);
            refreshedItems.add(item);
            TaskEntry parent = item.parent();
            if (parent != null) {
                refreshItem(parent, false, refreshedItems);
            }
        }
    }
}