package com.trajan.negentropy.client.controller.data;

import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
public class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
    // TODO: Data Provider rework
    private static final Logger logger = LoggerFactory.getLogger(TaskEntryDataProvider.class);

    @Autowired private QueryService queryService;

    private final Map<TaskID, Set<TaskEntry>> cachedTaskEntriesByChildTaskId = new HashMap<>();
    private final Map<TaskID, Task> cachedTasks = new HashMap<>();

    @Getter
    private TaskEntry baseEntry = null;
    @Getter
    private TaskFilter filter = new TaskFilter();

    public void setBaseEntry(TaskEntry baseEntry) {
        this.baseEntry = baseEntry;
        this.refreshAll();
    }

    @Override
    public int getChildCount(HierarchicalQuery<TaskEntry, Void> query) {
        if (query.getParent() == null) {
            if (baseEntry == null) {
                return queryService.fetchRootCount(filter);
            } else {
                return queryService.fetchChildCount(baseEntry.node().child().id(), filter);
            }
        }
        return queryService.fetchChildCount(query.getParent().node().child().id(), filter);
    }

    @Override
    public boolean hasChildren(TaskEntry taskEntry) {
        return taskEntry.node().child().hasChildren();
    }

    public Task getTask(TaskID taskId) {
        Task task =  cachedTasks.containsKey(taskId) ?
                cachedTasks.get(taskId) :
                queryService.fetchTask(taskId);
        cachedTasks.put(task.id(), task);
        return task;
    }

    @Override
    protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
        // TODO: Integrate query offset and limit into queryService
        // TODO: Cache tasks somehow if possible?
        TaskEntry parent = query.getParent();

        Stream<TaskNode> nodeStream;
        if (parent == null) {
            nodeStream = (baseEntry == null) ?
                    queryService.fetchRootNodes(filter) :
                    queryService.fetchChildNodes(baseEntry.node().child().id(), filter);
        } else {
            nodeStream = queryService.fetchChildNodes(parent.node().child().id(), filter);
        }

        return nodeStream
                .map(node -> {
                    TaskEntry entry = new TaskEntry(
                            parent,
                            node,
                            queryService.fetchNetTimeDuration(node.child().id()));
                    // TODO: Fetch a list of time durations, or handle elegantly somehow instead of a barrage of queries
                    cachedTaskEntriesByChildTaskId.computeIfAbsent(
                            node.child().id(), k -> new HashSet<>()).add(entry);
                    cachedTasks.put(node.child().id(), node.child());
                    return entry;
                });
    }

    public void refreshMatchingItems(TaskID id, boolean refreshAncestors) {
        cachedTasks.remove(id);
        Task task = queryService.fetchTask(id);
        if (cachedTaskEntriesByChildTaskId.containsKey(id)) {
            for (TaskEntry entry : cachedTaskEntriesByChildTaskId.get(id)) {
                entry.node().child(task);
                this.refreshItem(entry);
                if (refreshAncestors) {
                    TaskEntry parent = entry.parent();
                    while (parent != null) {
                        this.refreshItem(parent);
                        parent = parent.parent();
                    }
                }
            }
        } else {
            logger.error("Task entries do not contain key " + id);
        }
    }

    @Override
    public void refreshAll() {
        super.refreshAll();
    }

    public void reset() {
        cachedTaskEntriesByChildTaskId.clear();
        cachedTasks.clear();
    }
}