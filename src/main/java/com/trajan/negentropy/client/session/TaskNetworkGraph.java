package com.trajan.negentropy.client.session;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Getter
@Benchmark(millisFloor = 1)
public class TaskNetworkGraph implements Serializable {
    private static final long serialVersionUID = 1L;
     private final transient SessionLogger log = new SessionLogger();

    @Autowired protected transient SessionServices services;
    @Autowired protected transient UserSettings settings;
    @Setter protected transient TaskEntryDataProvider taskEntryDataProvider;

    private SyncID syncId;
    @Autowired private transient ServerBroadcaster serverBroadcaster;
    @Getter(AccessLevel.NONE)
    private Registration broadcastRegistration;
    @Autowired private VaadinSession session;

    private MutableNetwork<TaskID, LinkID> network;

    private Map<TaskID, Task> taskMap = new HashMap<>();
    private Map<LinkID, TaskNode> nodeMap = new HashMap<>();
    private Map<TagID, Tag> tagMap = new HashMap<>();
    private Multimap<TaskID, Tag> taskTagMap = HashMultimap.create();
    private MultiValueMap<TaskID, LinkID> nodesByTaskMap = new LinkedMultiValueMap<>();

    private AtomicReference<NetDurationInfo> netDurationInfo = new AtomicReference<>(null);

    public TaskNetworkGraph syncId(SyncID syncId) {
        log.trace("Previous syncId: " + this.syncId + ", new syncId: " + syncId);
        this.syncId = syncId;
        return this;
    }

    @PostConstruct
    public synchronized void init() {
        network = NetworkBuilder.directed()
                .allowsParallelEdges(true)
                .edgeOrder(ElementOrder.unordered())
                .build();
        taskMap = new HashMap<>();
        nodeMap = new HashMap<>();
        this.refreshTags();
        syncId(services.query().currentSyncId());
        log.info("Initial sync id: {}", this.syncId.val());
        services.query().fetchNodesGroupedHierarchically(null)
                .forEach(this::addTaskNode);

        if (settings != null) {
            this.syncNetDurations(this.syncId, settings.filter());
        } else {
            this.syncNetDurations(this.syncId, null);
        }
        String address = session != null ? session.getBrowser().getAddress() : null;
        String browser = session != null ? session.getBrowser().getBrowserApplication() : null;

        log.info("Session: " + address + " using " + browser);
        if (this.serverBroadcaster != null) {
            log.info("Registering sync with server broadcaster");
            broadcastRegistration = serverBroadcaster.register(this::sync);
        }
        log.info("Initialized TaskNetworkGraph with {} nodes", network.nodes().size());
    }

    @PreDestroy
    public synchronized void destroy() {
        broadcastRegistration.remove();
        broadcastRegistration = null;
    }

    public synchronized void reset() {
        tagMap = new HashMap<>();
        nodesByTaskMap = new LinkedMultiValueMap<>();
        init();
    }

    private void refreshTags() {
        services.query().fetchAllTags().forEach(tag -> {
            tagMap.put(tag.id(), tag);
            services.query().fetchTaskIdsByTagId(tag.id())
                    .forEach(task -> taskTagMap.put(task, tagMap.get(tag.id())));
        });
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

    public synchronized void addTaskNode(TaskNode node) {
        TaskID parentId = node.parentId() == null
                ? TaskID.nil()
                : node.parentId();
        network.addNode(parentId);
        network.addNode(node.child().id());
        network.addEdge(parentId, node.child().id(), node.linkId());

        if (node.task().tags() == null) {
            node.task().tags(Set.copyOf(taskTagMap.get(node.task().id())));
        } else {
            node.task().tags().forEach(tag -> tagMap.put(tag.id(), tag));
            taskTagMap.replaceValues(node.task().id(), node.task().tags());
        }

        nodeMap.put(node.linkId(), node);
        nodesByTaskMap.add(node.child().id(), node.linkId());
        addTask(node.child());
    }

    public synchronized void addTask(Task task) {
        if (task.tags() == null) {
            task.tags(Set.copyOf(taskTagMap.get(task.id())));
        } else {
            task.tags().forEach(tag -> tagMap.put(tag.id(), tag));
            taskTagMap.replaceValues(task.id(), task.tags());
        }

        taskMap.put(task.id(), task);
        nodesByTaskMap.getOrDefault(task.id(), List.of()).forEach(
                linkId -> {
                    if (nodeMap.containsKey(linkId)) {
                        TaskNode node = nodeMap.get(linkId).child(task);
                        nodeMap.put(linkId, node);
                    }
                });
    }

    public synchronized void removeTask(TaskID taskId) {
        taskMap.remove(taskId);
        nodesByTaskMap.remove(taskId);
    }

    public synchronized void removeTaskNode(LinkID linkId) {
        network.removeEdge(linkId);
        try {
            nodesByTaskMap.remove(nodeMap.get(linkId).task().id(), linkId);
        } catch (NullPointerException e) {
            log.warn("Error removing task node", e);
        }
        nodeMap.remove(linkId);
    }

    public synchronized Stream<TaskNode> getChildren(TaskID parentId, List<LinkID> filteredLinks, Integer offset, Integer limit) {
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

    public synchronized List<LinkID> getFilteredLinks(NestableTaskNodeTreeFilter filter) {
        log.debug("Getting filtered links with filter " + filter);
        return (filter != null && filter.nested() && filter.name() != null && !filter.name().isBlank())
                ? services.query().fetchAllNodesNestedAsIds(filter).toList()
                : services.query().fetchAllNodesAsIds(filter).toList();
    }

    private void syncNetDurations(SyncID syncId, TaskNodeTreeFilter filter) {
        if ((settings == null || settings.areNetDurationsVisible())
                && (syncId != this.syncId || this.netDurationInfo.get() == null)) {
            log.debug("Syncing net durations with filter " + filter);
            filterMap.clear();
            this.netDurationInfo.set(filterMap.compute(NonSpecificTaskNodeTreeFilter.parse(filter), (f, x) ->
                    services.query().fetchNetDurationInfo(f)));
        } else {
            this.netDurationInfo.set(null);
            log.debug("Not syncing net durations with filter " + filter);
        }
    }

    public synchronized void getNetDurations(TaskNodeTreeFilter filter) {
        if (settings.areNetDurationsVisible()) {
            log.debug("Getting net durations with filter " + filter);
            this.netDurationInfo.set(filterMap.computeIfAbsent(NonSpecificTaskNodeTreeFilter.parse(filter), f ->
                    services.query().fetchNetDurationInfo(f)));
        } else {
            this.netDurationInfo.set(null);
            log.debug("Not setting net durations with filter " + filter);
        }
    }

    public Set<Tag> getTags(TaskID task) {
        return Set.copyOf(taskTagMap.get(task));
    }

    private Map<NonSpecificTaskNodeTreeFilter, NetDurationInfo> filterMap = new HashMap<>();

    public synchronized void sync(SyncRecord aggregateSyncRecord) {
        Stopwatch stopWatch = Stopwatch.createStarted();
        if (!this.syncId.equals(aggregateSyncRecord.id())) {
            List<Change> changes = aggregateSyncRecord.changes();
            log.info("Syncing with sync id {}, {} changes, current sync id: {}",
                    aggregateSyncRecord.id(), changes.size(), this.syncId);

            CompletableFuture<Void> netDurationFuture;
            if (settings != null) {
                netDurationFuture = CompletableFuture.runAsync(() -> {
                    this.syncNetDurations(aggregateSyncRecord.id(), settings.filter());
                    this.refreshAll();
                });
            } else {
                netDurationFuture = CompletableFuture.runAsync(() -> {
                    this.syncNetDurations(aggregateSyncRecord.id(), null);
                    this.refreshAll();
                });
            }

            Map<TaskID, Task> taskMap = this.taskMap;
            Map<LinkID, TaskNode> nodeMap = this.nodeMap;

            // ID, Boolean = true to recurse through all children, false for only that single entry
            Map<LinkID, Boolean> pendingNodeRefresh = new HashMap<>();
            boolean refreshAll = false;
            boolean refreshTags = false;

            for (Change change : changes) {
                if (change instanceof MergeChange<?> mergeChange) {
                    Object mergeData = mergeChange.data();
                    if (mergeData instanceof Task task) {
                        log.debug("Got merged task {}", task);
                        taskMap.put(task.id(), task);
                        task.tags().forEach(tag -> tagMap.put(tag.id(), tag));
                        taskTagMap.replaceValues(task.id(), task.tags());
                        for (LinkID linkId : this.nodesByTaskMap().getOrDefault(task.id(), List.of())) {
                            pendingNodeRefresh.put(linkId, false);
                        }
                    } else if (mergeData instanceof TaskNode node) {
                        log.debug("Got merged node {}", node);
                        nodeMap.put(node.id(), node);
                        taskMap.put(node.task().id(), node.task());
                        pendingNodeRefresh.put(node.id(), false);
                    } else if (mergeData instanceof Tag) {
                        refreshTags = true;
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
                        refreshAll = true;
                    } else if (persistData instanceof Tag) {
                        refreshTags = true;
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
                        this.removeTaskNode(linkId);
                        refreshAll = true;
                    } else if (deleteData instanceof TagID) {
                        refreshTags = true;
                    } else {
                        throw new IllegalStateException("Unexpected value: " + deleteChange.data());
                    }
                } else {
                    log.warn("Unknown change type: {}", change);
                }

                this.syncId(aggregateSyncRecord.id());
            }

            CompletableFuture<Void> tagFuture = null;
            if (refreshTags) {
                tagFuture = CompletableFuture.runAsync(this::refreshTags);
            }

            if (refreshAll) {
                taskEntryDataProvider.refreshAll();
                if (!((tagFuture != null && tagFuture.isDone())
                        && netDurationFuture.isDone())) {
                    CompletableFuture.runAsync(this::refreshAll);
                }
            } else {
                taskEntryDataProvider.refreshNodes(pendingNodeRefresh);
                if (!((tagFuture != null && tagFuture.isDone())
                        && netDurationFuture.isDone())) {
                    CompletableFuture.runAsync(() -> this.refreshNodes(pendingNodeRefresh));
                }
            }

            stopWatch.stop();
        } else {
            log.debug("No sync required, sync ID's match");
        }
    }

    private synchronized void refreshAll() {
        taskEntryDataProvider.refreshAll();
        log.debug("Synchronized refresh");
    }

    private synchronized void refreshNodes(Map<LinkID, Boolean> pendingNodeRefresh) {
        taskEntryDataProvider.refreshNodes(pendingNodeRefresh);
        log.debug("Synchronized refresh");
    }
}
