package com.trajan.negentropy.client.tree.data;

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

    private final QueryService queryService;

    @Getter
    private final Map<TaskID, Set<TaskEntry>> cachedTaskEntriesByChildTaskId = new HashMap<>();
    private final Map<TaskID, Task> cachedTasks = new HashMap<>();

    @Getter
    private TaskEntry baseEntry = null;
    @Getter
    private TaskFilter activeFilter = new TaskFilter();

    public TaskEntryDataProvider(QueryService queryService) {
        this.queryService = queryService;
    }

    public void setBaseEntry(TaskEntry baseEntry) {
        this.baseEntry = baseEntry;
        this.refreshAll();
    }

    @Override
    public int getChildCount(HierarchicalQuery<TaskEntry, Void> query) {
        if (query.getParent() == null) {
            if (baseEntry == null) {
                return queryService.fetchRootCount(activeFilter);
            } else {
                return queryService.fetchChildCount(baseEntry.task().id(), activeFilter);
            }
        }
        return queryService.fetchChildCount(query.getParent().task().id(), activeFilter);
    }

    @Override
    public boolean hasChildren(TaskEntry taskEntry) {
        return taskEntry.hasChildren();
    }

    @Override
    protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
        // TODO: Integrate query offset and limit into queryService
        // TODO: Cache tasks somehow if possible?
        TaskEntry parent = query.getParent();

        Stream<TaskNode> nodeStream;
        if (parent == null) {
            nodeStream = (baseEntry == null) ?
                    queryService.fetchRootNodes(activeFilter) :
                    queryService.fetchChildNodes(baseEntry.task().id(), activeFilter);
        } else {
            nodeStream = queryService.fetchChildNodes(parent.task().id(), activeFilter);
        }

        return nodeStream
                .map(node -> {
                    Task task = cachedTasks.containsKey(node.childId()) ?
                            cachedTasks.get(node.childId()) :
                            queryService.fetchTask(node.childId());

                    boolean hasChildren = queryService.hasChildren(task.id(), activeFilter);

                    TaskEntry entry = new TaskEntry(
                            parent,
                            node,
                            task,
                            queryService.fetchNetTimeDuration(node.childId()),
                            hasChildren);
                    cachedTaskEntriesByChildTaskId.computeIfAbsent(
                            entry.task().id(), k -> new HashSet<>()).add(entry);
                    cachedTasks.put(task.id(), task);
                    return entry;
                });
    }

    public void refreshMatchingItems(TaskID id, boolean ancestors) {
        cachedTasks.remove(id);
        Task task = queryService.fetchTask(id);
        if (cachedTaskEntriesByChildTaskId.containsKey(id)) {
            for (TaskEntry entry : cachedTaskEntriesByChildTaskId.get(id)) {
                entry.task(task);
                this.refreshItem(entry);
                if (ancestors) {
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