package com.trajan.negentropy.client.tree.data;

import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@UIScope
public class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntryDataProvider.class);

    private final QueryService queryService;

    private final Map<TaskID, Set<TaskEntry>> entriesByChildId = new HashMap<>();
//    private Map<TaskID, Duration> taskTimeEstimateMap; TODO: Implement

    @Getter
    private TaskEntry baseEntry = null;
    @Getter
    private TaskFilter filter = new TaskFilter();

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
                return queryService.fetchRootCount(filter);
            } else {
                return queryService.fetchChildCount(baseEntry.task().id(), filter);
            }
        }
        return queryService.fetchChildCount(query.getParent().task().id(), filter);
    }

    @Override
    public boolean hasChildren(TaskEntry taskEntry) {
        return queryService.hasChildren(taskEntry.task().id(), filter);
    }

    @Override
    protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
        // TODO: Integrate query offset and limit into queryService

        TaskEntry parent = query.getParent();
        Stream<TaskNode> nodeStream;
        Map<TaskID, Duration> taskTimeEstimates;
        if (parent == null) {
            nodeStream = (baseEntry == null) ?
                    queryService.fetchRootNodes(filter) :
                    queryService.fetchChildNodes(baseEntry.task().id(), filter);
        } else {
            nodeStream = queryService.fetchChildNodes(parent.task().id(), filter);
        }

        return nodeStream
                .map(node -> {
                    TaskEntry entry = new TaskEntry(
                            parent,
                            node,
                            queryService.fetchTask(node.childId()),
                            queryService.fetchNetTimeDuration(node.childId()));
                    entriesByChildId.computeIfAbsent(
                            entry.task().id(), k -> new HashSet<>()).add(entry);
                    return entry;
                });
    }

    public void refreshMatchingItems(TaskID id, boolean ancestors) {
        Task task = queryService.fetchTask(id);
        for (TaskEntry entry : entriesByChildId.get(id)) {
            entry.task(task);
            this.refreshItem(entry);
            if (ancestors) {
                TaskEntry parent = entry.parent();
                while (parent != null) {
                    this.refreshItem(parent);
                    parent = entry.parent();
                }
            }
        }
    }

    @Override
    public void refreshAll() {
        super.refreshAll();
    }
}