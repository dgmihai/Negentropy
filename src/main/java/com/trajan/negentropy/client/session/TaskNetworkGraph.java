package com.trajan.negentropy.client.session;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.logger.SessionLogger;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter.NestableTaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.backend.NetDurationService.NetDurationInfo;
import com.trajan.negentropy.server.broadcaster.ServerBroadcaster;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Getter
@Benchmark
public class TaskNetworkGraph {
    private final SessionLogger log = new SessionLogger();

    @Autowired protected SessionServices services;
    @Autowired protected UserSettings settings;
    @Setter protected TaskEntryDataProvider taskEntryDataProvider;

    private SyncID syncId;
    @Autowired private ServerBroadcaster serverBroadcaster;
    @Getter(AccessLevel.NONE)
    private Registration broadcastRegistration;
    @Autowired private VaadinSession session;

    private MutableNetwork<TaskID, LinkID> network;

    private Map<TaskID, Task> taskMap = new HashMap<>();
    private Map<LinkID, TaskNode> nodeMap = new HashMap<>();
    private Map<TagID, Tag> tagMap = new HashMap<>();
    private MultiValueMap<TaskID, LinkID> nodesByTaskMap = new LinkedMultiValueMap<>();

    private NetDurationInfo netDurationInfo;

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
        if (settings != null) {
            this.syncNetDurations(this.syncId, settings.filter());
        } else {
            this.syncNetDurations(this.syncId, null);
        }
        String address = session != null ? session.getBrowser().getAddress() : null;
        String browser = session != null ? session.getBrowser().getBrowserApplication() : null;

        log.info("Session: " + address + " using " + browser);
        if (this.serverBroadcaster != null) broadcastRegistration = serverBroadcaster.register(this::sync);
        log.info("Initialized TaskNetworkGraph with {} nodes", network.nodes().size());
    }

    @PreDestroy
    public void destroy() {
        broadcastRegistration.remove();
        broadcastRegistration = null;
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

    public int getChildCount(TaskID parentId, List<LinkID> filteredLinks, Integer offset, Integer limit) {
        Stream<LinkID> children = (parentId != null
                ? network.outEdges(parentId)
                : network.outEdges(TaskID.nil()))
                .stream()
                .filter(linkId -> filteredLinks == null || filteredLinks.contains(linkId));
        children = (offset != null) ? children.skip(offset) : children;
        children = (limit != null) ? children.limit(limit) : children;
        return (int) children.count();
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

    public Stream<TaskNode> getChildren(TaskID parentId, List<LinkID> filteredLinks, Integer offset, Integer limit) {
        log.debug("Getting children for parent " + taskMap.get(parentId) + " where filtered tasks "
                + (filteredLinks != null ? "count is " + filteredLinks.size() : "is null with offset " + offset +
                " and limit " + limit));
        try {
            Set<LinkID> children = parentId != null
                    ? network.outEdges(parentId)
                    : network.outEdges(TaskID.nil());
            Ordering<TaskNode> ordering = Ordering.natural().onResultOf(TaskNode::position);
            Stream<TaskNode> childStream = children.stream()
                    .map(nodeMap::get)
                    .filter(node ->
                            filteredLinks == null
                                    || filteredLinks.contains(node.linkId()))
                    .sorted(ordering);
            childStream = (offset != null) ? childStream.skip(offset) : childStream;
            childStream = (limit != null) ? childStream.limit(limit) : childStream;
            log.debug("Retrieving child links for parent " + taskMap.get(parentId));
            return childStream;
        } catch (IllegalArgumentException e) {
            log.error("Failed to fetch children with exception - the DB may be empty.", e);
            return Stream.of();
        }
    }

    public List<LinkID> getFilteredLinks(NestableTaskNodeTreeFilter filter) {
        log.debug("Getting filtered links with filter " + filter);
        return (filter.nested())
                ? services.query().fetchAllNodesNestedAsIds(filter).toList()
                : services.query().fetchAllNodesAsIds(filter).toList();
    }

    private void syncNetDurations(SyncID syncId, TaskNodeTreeFilter filter) {
        if (syncId != this.syncId || this.netDurationInfo == null) {
            log.debug("Syncing net durations with filter " + filter);
            filterMap.clear();
            this.netDurationInfo = filterMap.compute(NonSpecificTaskNodeTreeFilter.from(filter), (f, x) ->
                    services.query().fetchNetDurationInfo(f));
        }
    }

    public void getNetDurations(TaskNodeTreeFilter filter) {
        log.debug("Getting net durations with filter " + filter);
        this.netDurationInfo = filterMap.computeIfAbsent(NonSpecificTaskNodeTreeFilter.from(filter), f ->
                services.query().fetchNetDurationInfo(f));
    }

    private Map<NonSpecificTaskNodeTreeFilter, NetDurationInfo> filterMap = new HashMap<>();

    public synchronized void sync(SyncRecord aggregateSyncRecord) {
        Stopwatch stopWatch = Stopwatch.createStarted();
        if (this.syncId != aggregateSyncRecord.id()) {
            List<Change> changes = aggregateSyncRecord.changes();
            log.info("Syncing with sync id {}, {} changes, current sync id: {}",
                    aggregateSyncRecord.id(), changes.size(), this.syncId);

            if (settings != null) {
                this.syncNetDurations(aggregateSyncRecord.id(), settings.filter());
            } else {
                this.syncNetDurations(aggregateSyncRecord.id(), null);
            }

            Map<TaskID, Task> taskMap = this.taskMap;
            Map<LinkID, TaskNode> nodeMap = this.nodeMap;

            // ID, Boolean = true to recurse through all children, false for only that single entry
            Map<LinkID, Boolean> pendingNodeRefresh = new HashMap<>();
            boolean refreshAll = false;

            log.debug("MARK 1: {} ms", stopWatch.elapsed().toMillis());
            for (Change change : changes) {
                if (change instanceof MergeChange<?> mergeChange) {
                    Object mergeData = mergeChange.data();
                    if (mergeData instanceof Task task) {
                        log.debug("Got merged task {}", task);
                        taskMap.put(task.id(), task);
                        for (LinkID linkId : this.nodesByTaskMap().getOrDefault(task.id(), List.of())) {
                            pendingNodeRefresh.put(linkId, false);
                        }
                    } else if (mergeData instanceof TaskNode node) {
                        log.debug("Got merged node {}", node);
                        nodeMap.put(node.id(), node);
                        taskMap.put(node.task().id(), node.task());
                        pendingNodeRefresh.put(node.id(), false);
                    } else if (mergeData instanceof Tag) {
                        this.refreshTags();
                    } else {
                        throw new IllegalStateException("Unexpected value: " + mergeChange.data());
                    }
                } else if (change instanceof PersistChange<?> persist) {
                    Object persistData = persist.data();
                    if (persistData instanceof Task task) {
                        log.debug("Got persisted task {}", task);
                        this.addTask(task);
                        for (LinkID linkId : this.nodesByTaskMap().getOrDefault(task.id(), List.of())) {
                            pendingNodeRefresh.put(linkId, false);
                        }
                    } else if (persistData instanceof TaskNode node) {
                        log.debug("Got persisted node {}", node);
                        this.addTaskNode(node);
                        pendingNodeRefresh.put(node.id(), false);
                        TaskID parentId = node.parentId();
                        if (parentId != null) {
                            for (LinkID linkId : this.nodesByTaskMap().getOrDefault(parentId, List.of())) {
                                pendingNodeRefresh.put(linkId, false);
                            }
                        } else {
                            refreshAll = true;
                            break;
                        }
                    } else if (persistData instanceof Tag) {
                        this.refreshTags();
                    } else {
                        throw new IllegalStateException("Unexpected value: " + persist.data());
                    }
                } else if (change instanceof DeleteChange<?> deleteChange) {
                    Object deleteData = deleteChange.data();
                    if (deleteData instanceof TaskID taskId) {
                        log.debug("Got deleted task {}", taskId);
                        this.removeTask(taskId);
                    } else if (deleteData instanceof LinkID linkId) {
                        log.debug("Got deleted node {}", linkId);
                        TaskNode deletedNode = nodeMap.getOrDefault(linkId, null);
                        if (deletedNode == null) {
                            log.warn("Deleted node {} not found in node map", linkId);
                            // Refresh all nodes
                            refreshAll = true;
                            break;
                        } else {
                            TaskID parentId = deletedNode.parentId();
                            if (parentId != null) {
                                for (LinkID lid : this.nodesByTaskMap().getOrDefault(parentId, List.of())) {
                                    pendingNodeRefresh.put(lid, true);
                                }
                            } else {
                                refreshAll = true;
                                break;
                            }
                        }
                        this.removeTaskNode(linkId);
                    } else if (deleteData instanceof TagID) {
                        this.refreshTags();
                    } else {
                        throw new IllegalStateException("Unexpected value: " + deleteChange.data());
                    }
                } else {
                    log.warn("Unknown change type: {}", change);
                }

                this.syncId(aggregateSyncRecord.id());
            }

            log.debug("MARK 2: {} ms", stopWatch.elapsed().toMillis());
            if (refreshAll) {
                taskEntryDataProvider.refreshAll();
            } else {
                taskEntryDataProvider.refreshNodes(pendingNodeRefresh);
            }
            log.debug("MARK 3: {} ms", stopWatch.elapsed().toMillis());
            stopWatch.stop();
        } else {
            log.debug("No sync required, sync ID's match");
        }
    }
}
