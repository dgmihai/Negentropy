package com.trajan.negentropy.client.controller;

import com.google.common.collect.Ordering;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringComponent
@VaadinSessionScope
@Getter
@Slf4j
@Benchmark(millisFloor = 10)
public class TaskNetworkGraph {
    @Autowired protected SessionServices services;

    private MutableNetwork<TaskID, LinkID> network;

    private SyncID syncId;

    private Map<TaskID, Task> taskMap = new HashMap<>();
    private Map<LinkID, TaskNode> nodeMap = new HashMap<>();
    private Map<TagID, Tag> tagMap = new HashMap<>();
    private MultiValueMap<TaskID, LinkID> nodesByTaskMap = new LinkedMultiValueMap<>();
    @Setter
    private Map<TaskID, Duration> netDurations;

    public TaskNetworkGraph syncId(SyncID syncId) {
        log.trace("Previous syncId: " + this.syncId + ", new syncId: " + syncId);
        this.syncId = syncId;
        return this;
    }

    @PostConstruct
    public void init() {
        network = NetworkBuilder.directed()
                .allowsParallelEdges(true)
                .edgeOrder(ElementOrder.unordered())
                .build();
        taskMap = new HashMap<>();
        nodeMap = new HashMap<>();
        refreshTags();
        syncId(services.query().currentSyncId());
        log.info("Initial sync id: {}", this.syncId.val());
        services.query().fetchDescendantNodes(null, null)
                .forEach(this::addTaskNode);
        netDurations = services.query().fetchAllNetDurations(null);
        log.info("Initialized TaskNetworkGraph with {} nodes", network.nodes().size());
    }

    public void reset() {
        tagMap = new HashMap<>();
        nodesByTaskMap = new LinkedMultiValueMap<>();
        init();
    }

    public void refreshTags() {
        tagMap = services.query().fetchAllTags().collect(Collectors.toMap(
                Tag::id, tag -> tag));
    }

    public int getChildCount(TaskID parentId, List<LinkID> filteredLinks) {
        return getChildren(parentId, filteredLinks).size();
    }

    public boolean hasChildren(TaskID parentId) {
        return network.outDegree(parentId) > 0;
    }

    public void addTaskNode(TaskNode node) {
        TaskID parentId = node.parentId() == null
                ? TaskID.nil()
                : node.parentId();
        network.addNode(parentId);
        network.addNode(node.child().id());
        network.addEdge(parentId, node.child().id(), node.linkId());
        nodeMap.put(node.linkId(), node);
        nodesByTaskMap.add(node.child().id(), node.linkId());
        addTask(node.child());
    }

    public void addTask(Task task) {
        taskMap.put(task.id(), task);
        nodesByTaskMap.getOrDefault(task.id(), List.of()).forEach(
                linkId -> {
                    if (nodeMap.containsKey(linkId)) {
                        TaskNode node = nodeMap.get(linkId).child(task);
                        nodeMap.put(linkId, node);
                    }
                });
    }

    public void removeTask(TaskID taskId) {
        taskMap.remove(taskId);
        nodesByTaskMap.remove(taskId);
    }

    public void removeTaskNode(LinkID linkId) {
        network.removeEdge(linkId);
        try {
            nodesByTaskMap.remove(nodeMap.get(linkId).task().id(), linkId);
        } catch (NullPointerException e) {
            log.warn("Error removing task node", e);
        }
        nodeMap.remove(linkId);
    }

    public List<TaskNode> getChildren(TaskID parentId, List<LinkID> filteredLinks) {
        log.debug("Getting children for parent " + taskMap.get(parentId) + " where filtered tasks "
                + (filteredLinks != null ? "count is " + filteredLinks.size() : "is null"));
        try {
            Set<LinkID> children = parentId != null
                    ? network.outEdges(parentId)
                    : network.outEdges(TaskID.nil());
            Ordering<TaskNode> ordering = Ordering.natural().onResultOf(TaskNode::position);
            List<TaskNode> filteredChildren = ordering.sortedCopy(children.stream()
                    .map(nodeMap::get)
                    .filter(node ->
                            filteredLinks == null
                            || filteredLinks.contains(node.linkId()))
                    .toList());
            log.debug("Got " + filteredChildren.size() + " child links for parent " + taskMap.get(parentId));
            return filteredChildren;
        } catch (IllegalArgumentException e) {
            log.error("Failed to fetch children with exception - the DB may be empty.", e);
            return List.of();
        }
    }

    public List<LinkID> getFilteredLinks(List<LinkID> previous, TaskNodeTreeFilter filter) {
        log.debug("Getting filtered links with filter " + filter);
        return services.query().fetchAllNodesAsIds(filter)
                .toList();
    }
}
