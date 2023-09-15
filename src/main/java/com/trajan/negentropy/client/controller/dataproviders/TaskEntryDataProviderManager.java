package com.trajan.negentropy.client.controller.dataproviders;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.controller.SessionServices;
import com.trajan.negentropy.client.controller.TaskNetworkGraph;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
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
@Benchmark(millisFloor = 10)
public class TaskEntryDataProviderManager {
    @Autowired private SessionServices services;
    @Autowired private TaskNetworkGraph taskNetworkGraph;

    // ID, Boolean = true to recurse through all children, false for only that single entry
    @Getter private final Map<LinkID, Boolean> pendingNodeRefresh = new HashMap<>();

    @Getter private final Set<TaskEntryDataProvider> allProviders = new LinkedHashSet<>();

    public void refreshQueuedItems() {
        log.debug("Refreshing providers");
        for (TaskEntryDataProvider provider : allProviders) {
            provider.refreshNodes(this.pendingNodeRefresh);
        }

        this.pendingNodeRefresh.clear();
    }

    public void refreshAllProviders() {
        log.debug("Refreshing all providers");
        for (TaskEntryDataProvider provider : allProviders) {
            provider.refreshAll();
        }

        this.pendingNodeRefresh.clear();
    }

    public TaskEntryDataProvider create() {
        TaskEntryDataProvider gridDataProvider = new TaskEntryDataProvider(taskNetworkGraph);
        this.allProviders().add(gridDataProvider);
        return gridDataProvider;
    }

    public void setFilter(TaskNodeTreeFilter filter) {
        allProviders().forEach(provider -> provider.setFilter(filter));
    }

    @Slf4j
    @Getter
    @Benchmark(millisFloor = 10)
    public static class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
        private TaskNetworkGraph networkGraph;

        @Getter private final MultiValueMap<TaskID, TaskEntry> taskTaskEntriesMap = new LinkedMultiValueMap<>();
        @Getter private final MultiValueMap<LinkID, TaskEntry> linkTaskEntriesMap = new LinkedMultiValueMap<>();

        private TaskEntry rootEntry;
        private List<LinkID> filteredLinks;
        private TaskNodeTreeFilter filter;

        public TaskEntryDataProvider(TaskNetworkGraph networkGraph) {
            log.info("TaskEntryGridDataProvider init");
            this.networkGraph = networkGraph;
            this.rootEntry = null;
            this.setFilter(networkGraph.settings().filter());
        }

        public void refreshNodes(Map<LinkID, Boolean> linkIdMap) {
            for (Entry<LinkID, Boolean> mapEntry : linkIdMap.entrySet()) {
                LinkID id = mapEntry.getKey();
                log.debug("Refreshing node with id {}", id);
                if (linkTaskEntriesMap.containsKey(id)) {
                    List<TaskEntry> taskEntries = linkTaskEntriesMap.get(id);

                    for (TaskEntry entry : taskEntries) {
                        entry.node((networkGraph.nodeMap().get(entry.node().id())));
                        entry.node().child(networkGraph.taskMap().get(entry.node().child().id()));
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
            log.debug("Refreshing all");
            refreshFilter();
            super.refreshAll();
        }
        public void refreshFilter() {
            log.debug("Refreshing filter");
            this.filteredLinks = networkGraph.getFilteredLinks(filteredLinks, filter);
        }

        public void setFilter(TaskNodeTreeFilter filter) {
            this.filter = filter;
            this.filteredLinks = networkGraph.getFilteredLinks(filteredLinks, filter);
            super.refreshAll();
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
