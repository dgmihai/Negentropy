package com.trajan.negentropy.client.session;

import com.google.common.collect.Ordering;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.sessionlogger.SessionLogged;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;
import com.trajan.negentropy.client.sessionlogger.SessionLoggerFactory;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.ServerBroadcaster;
import com.trajan.negentropy.server.backend.NetDurationService.NetDurationInfo;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Getter
@Benchmark(millisFloor = 10)
public class TaskNetworkGraph implements SessionLogged {
    @Autowired private SessionLoggerFactory loggerFactory;
    private SessionLogger log;

    @Autowired protected SessionServices services;
    @Autowired protected UserSettings settings;

    private SyncID syncId;
    @Autowired private ServerBroadcaster serverBroadcaster;
    @Getter(AccessLevel.NONE)
    private Registration broadcastRegistration;
    @Autowired private VaadinSession session;

    private TaskEntryDataProvider taskEntryDataProvider;

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
        log = getLogger(this.getClass());

        taskEntryDataProvider = new TaskEntryDataProvider();

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

    public List<LinkID> getFilteredLinks(List<LinkID> previous, TaskNodeTreeFilter filter) {
        log.debug("Getting filtered links with filter " + filter);
        return services.query().fetchAllNodesAsIds(filter)
                .toList();
    }

    public synchronized void syncNetDurations(SyncID syncId, TaskNodeTreeFilter filter) {
        if (syncId != this.syncId) {
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
                            // TODO: Needs to be fixed
//                        taskEntryDataProviderManager.pendingTaskRefresh().put(parentId, true);
                            refreshAll = true;
                        } else {
                            refreshAll = true;
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
                        } else {
                            TaskID parentId = deletedNode.parentId();
                            if (parentId != null) {
                                for (LinkID lid : this.nodesByTaskMap().getOrDefault(parentId, List.of())) {
                                    pendingNodeRefresh.put(lid, true);
                                }
                            } else {
                                refreshAll = true;
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

            if (refreshAll) {
                taskEntryDataProvider.refreshAll();
            } else {
                taskEntryDataProvider.refreshNodes(pendingNodeRefresh);
            }
        } else {
            log.debug("No sync required, sync ID's match");
        }
    }

    @Getter
    @Benchmark(millisFloor = 10) // Does not work since not Spring bean
    public class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
        private SessionLogger log;

        @Getter private final MultiValueMap<TaskID, TaskEntry> taskTaskEntriesMap = new LinkedMultiValueMap<>();
        @Getter private final MultiValueMap<LinkID, TaskEntry> linkTaskEntriesMap = new LinkedMultiValueMap<>();

        private TaskEntry rootEntry;
        private List<LinkID> filteredLinks;
        private TaskNodeTreeFilter filter;

        @Autowired
        public TaskEntryDataProvider() {
            log = getLogger(this.getClass());

            log.info("TaskEntryGridDataProvider init");
            this.rootEntry = null;
            if (settings != null) {
                this.setFilter(settings.filter());
            } else {
                this.setFilter(null);
            }
        }

        public void refreshNodes(Map<LinkID, Boolean> linkIdMap) {
            log.debug("Refreshing nodes");
            session.access(() -> {
                for (Entry<LinkID, Boolean> mapEntry : linkIdMap.entrySet()) {
                    LinkID id = mapEntry.getKey();
                    log.debug("Refreshing node with id {}", id);
                    if (linkTaskEntriesMap.containsKey(id)) {
                        List<TaskEntry> taskEntries = linkTaskEntriesMap.get(id);

                        for (TaskEntry entry : taskEntries) {
                            entry.node((nodeMap.get(entry.node().id())));
                            entry.node().child(taskMap.get(entry.node().child().id()));
                            this.refreshItem(entry, mapEntry.getValue());
                        }
                    }
                }
            });
        }

        public void rootEntry(TaskEntry rootEntry) {
            log.debug("Setting root entry: " + rootEntry);
            this.rootEntry = rootEntry;
            this.refreshAll();
        }

        public TaskID getRootTaskID() {
            return rootEntry != null ? rootEntry.task().id() : null;
        }

        @Override
        public void refreshAll() {
            log.debug("Refreshing all");

            session.access(() -> {
                refreshFilter();
                super.refreshAll();
            });
        }
        public void refreshFilter() {
            log.debug("Refreshing filter");
            this.filteredLinks = getFilteredLinks(filteredLinks, filter);
        }

        public void setFilter(TaskNodeTreeFilter filter) {
            this.filter = filter;
            this.filteredLinks = getFilteredLinks(filteredLinks, filter);
            super.refreshAll();
            getNetDurations(filter);
        }

        @Override
        public boolean isInMemory() {
            return true;
        }

        @Override
        public int getChildCount(HierarchicalQuery<TaskEntry, Void> query) {
            log.trace("Getting child count for " + query.getParent());
            TaskID parentTaskID = query.getParent() != null ? query.getParent().task().id() : getRootTaskID();
            return TaskNetworkGraph.this.getChildCount(parentTaskID, this.filteredLinks, query.getOffset(), query.getLimit());
        }

        @Override
        protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
            TaskEntry parent = query.getParent() != null ? query.getParent() : rootEntry;
            TaskID parentTaskID = parent != null ? parent.task().id() : getRootTaskID();

            if (parent == null) {
                linkTaskEntriesMap.clear();
                taskTaskEntriesMap.clear();
            }

            log.trace("Fetching children for parent " + parent);
            return getChildren(parentTaskID, this.filteredLinks, query.getOffset(), query.getLimit())
                    .map(node -> {
                        log.trace("Fetching child: " + node);
                        TaskEntry entry = new TaskEntry(parent, nodeMap.get(node.id()));taskTaskEntriesMap.add(node.task().id(), entry);
                        linkTaskEntriesMap.add(node.id(), entry);
                        log.trace("Adding new entry: " + entry);
                        return entry;
                    });
        }

        @Override
        public boolean hasChildren(TaskEntry item) {
            if (item == null) return true;
            boolean result = TaskNetworkGraph.this.hasChildren(item.task().id());
            log.trace("Item {} has children: {}", item.task().name(), result);
            return result;
        }
    }
}
