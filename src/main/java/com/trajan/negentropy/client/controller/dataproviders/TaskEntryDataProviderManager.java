package com.trajan.negentropy.client.controller.dataproviders;

import com.trajan.negentropy.client.controller.SessionServices;
import com.trajan.negentropy.client.controller.TaskNetworkGraph;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Slf4j
public class TaskEntryDataProviderManager {
    @Autowired private SessionServices services;
    @Autowired private TaskNetworkGraph taskNetworkGraph;

//    @Getter private final Set<TaskEntryTreeGrid> treeGrids = new HashSet<>();

//    public void resetAllData() {
//        log.debug("Refreshing all grids");
//        for (TaskEntryTreeGrid grid : treeGrids) {
//            grid.setData(grid.rootEntry());
//        }
//    }

    @Getter private final Set<TaskEntryDataProvider> allProviders = new LinkedHashSet<>();

    // ID, Boolean = true to recurse through all children, false for only that single entry
    @Getter private final Map<TaskID, Boolean> pendingTaskRefresh = new HashMap<>();
    @Getter private final Map<LinkID, Boolean> pendingNodeRefresh = new HashMap<>();

    public void refreshQueuedItems() {
        log.debug("Refreshing providers");
        for (TaskEntryDataProvider provider : allProviders) {
            provider.refreshTasks(pendingTaskRefresh);
            provider.refreshNodes(pendingNodeRefresh);
        }

        pendingTaskRefresh.clear();
        pendingNodeRefresh.clear();
    }

    public void refreshAllProviders() {
        log.debug("Refreshing all providers");
        for (TaskEntryDataProvider provider : allProviders) {
            provider.refreshAll();
        }

        pendingTaskRefresh.clear();
        pendingNodeRefresh.clear();
    }

    public TaskEntryDataProvider create() {
        TaskEntryDataProvider gridDataProvider = new TaskEntryDataProvider(taskNetworkGraph);
        this.allProviders().add(gridDataProvider);
        return gridDataProvider;
    }

    public void setFilter(TaskFilter filter) {
        allProviders().forEach(provider -> provider.setFilter(filter));
    }

    @Slf4j
    @Getter
    public static class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
        private TaskNetworkGraph networkGraph;

        @Getter private final MultiValueMap<TaskID, TaskEntry> taskTaskEntriesMap = new LinkedMultiValueMap<>();
        @Getter private final MultiValueMap<LinkID, TaskEntry> linkTaskEntriesMap = new LinkedMultiValueMap<>();

        private TaskEntry rootEntry;
        private List<LinkID> filteredLinks;
        private TaskFilter filter;

        public TaskEntryDataProvider(TaskNetworkGraph networkGraph) {
            log.info("TaskEntryGridDataProvider init");
            this.networkGraph = networkGraph;
            this.rootEntry(null);
            this.refreshAll();
        }

        public void refreshTasks(Map<TaskID, Boolean> taskIdMap) {
            for (Entry<TaskID, Boolean> mapEntry : taskIdMap.entrySet()) {
                TaskID id = mapEntry.getKey();
                if (taskTaskEntriesMap.containsKey(id)) {
                    List<TaskEntry> taskEntries = taskTaskEntriesMap.get(id);

                    for (TaskEntry entry : taskEntries) {
                        entry.node((networkGraph.nodeMap().get(entry.node().id())));
                        if (entry.node() != null) {
                            entry.node().child(networkGraph.taskMap().get(id));
                            if (rootEntry == null ||
                                    (mapEntry.getValue() && entry.node().id().equals(rootEntry.node().id()))) {
                                refreshAll();
                                return;
                            }
                            this.refreshItem(entry, mapEntry.getValue());
                        } else {
                            log.warn("TaskEntry {} has null node", entry);
                        }
                    }
                }
            }
        }

        public void refreshNodes(Map<LinkID, Boolean> linkIdMap) {
            for (Entry<LinkID, Boolean> mapEntry : linkIdMap.entrySet()) {
                LinkID id = mapEntry.getKey();
                log.debug("Refreshing node with id {}", id);
                if (linkTaskEntriesMap.containsKey(id)) {
                    List<TaskEntry> taskEntries = linkTaskEntriesMap.get(id);

                    for (TaskEntry entry : taskEntries) {
                        entry.node((networkGraph.nodeMap().get(entry.node().id())));
                        if (mapEntry.getValue() && entry.node().id().equals(rootEntry.node().id())) {
                            refreshAll();
                            return;
                        }
                        this.refreshItem(entry, mapEntry.getValue());
                    }
                }
            }
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
            refreshFilter();
            super.refreshAll();
        }
        public void refreshFilter() {
            this.filteredLinks = networkGraph.getFilteredLinks(getRootTaskID(), filter);
        }

        public void setFilter(TaskFilter filter) {
            this.filter = filter;
            this.filteredLinks = networkGraph.getFilteredLinks(getRootTaskID(), filter);
            this.refreshAll();
        }

        @Override
        public boolean isInMemory() {
            return true;
        }

        @Override
        public int getChildCount(HierarchicalQuery<TaskEntry, Void> query) {
            log.debug("Getting child count for " + query.getParent());
            TaskID parentTaskID = query.getParent() != null ? query.getParent().task().id() : getRootTaskID();
            return networkGraph.getChildCount(parentTaskID, this.filteredLinks);
        }


        @Override
        protected Stream<TaskEntry> fetchChildrenFromBackEnd(HierarchicalQuery<TaskEntry, Void> query) {
            TaskEntry parent = query.getParent() != null ? query.getParent() : rootEntry;
            TaskID parentTaskID = parent != null ? parent.task().id() : getRootTaskID();

            if (parent == null) {
                linkTaskEntriesMap.clear();
                taskTaskEntriesMap.clear();
            }

            log.debug("Fetching children for parent " + parent);
            return networkGraph.getChildren(parentTaskID, this.filteredLinks).stream()
                    .map(node -> {
                        log.trace("Fetching child: " + node);
                        TaskEntry entry = new TaskEntry(parent, networkGraph.nodeMap().get(node.id()));taskTaskEntriesMap.add(node.task().id(), entry);
                        linkTaskEntriesMap.add(node.id(), entry);
                        log.trace("Adding new entry: " + entry);
                        return entry;
                    });
        }

        @Override
        public boolean hasChildren(TaskEntry item) {
            boolean result = networkGraph.hasChildren(item.task().id());
            log.trace("Item {} has children: {}", item.task().name(), result);
            return result;
        }
    }
}
