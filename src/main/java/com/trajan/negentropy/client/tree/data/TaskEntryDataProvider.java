package com.trajan.negentropy.client.tree.data;

import com.trajan.negentropy.server.facade.TaskQueryService;
import com.trajan.negentropy.server.facade.model.LinkID;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskID;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Stream;

@UIScope
public class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntryDataProvider.class);

    private final TaskQueryService queryService;

    private Map<TaskID, Task> tasks;
    private Map<LinkID, TaskNode> nodes;

    @Getter
    private TaskEntry baseEntry = null;

    public TaskEntryDataProvider(TaskQueryService queryService) {
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
                return queryService.getRootCount();
            } else {
                return queryService.getChildCount(baseEntry.task().id());
            }
        }
        return queryService.getChildCount(query.getParent().task().id());
    }

    @Override
    public boolean hasChildren(TaskEntry taskEntry) {
        return queryService.hasChildren(taskEntry.task().id());
    }

    @Override
    protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
        // TODO: Integrate query offset and limit into queryService

        TaskEntry parent = query.getParent();
        Stream<TaskNode> nodeStream;
        if (parent == null) {
            nodeStream = (baseEntry == null) ?
                    queryService.getRootNodes().stream() :
                    queryService.getChildNodes(baseEntry.task().id()).stream();
        } else {
            nodeStream = queryService.getChildNodes(parent.task().id()).stream();
        }

        return nodeStream
                .map(node -> new TaskEntry(parent, node, queryService.getTask(node.childId())));
    }

//    @Override
//    public void refreshItem(TaskEntry item, boolean updateChildren) {
//        if (updateChildren) {
//
//        } else {
//            item = new TaskEntry(
//                    item.parent(),
//                    queryService.getNode(item.node().linkId()));
//        }
//    }
}