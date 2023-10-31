package com.trajan.negentropy.client.session;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.logger.SessionLogger;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Getter
@Benchmark
public class TaskEntryDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskEntry, Void> {
    private final SessionLogger log = new SessionLogger();

    @Autowired private TaskNetworkGraph taskNetworkGraph;

    @Autowired private UserSettings settings;
    @Autowired private VaadinSession session;

    @Getter private final MultiValueMap<TaskID, TaskEntry> taskTaskEntriesMap = new LinkedMultiValueMap<>();
    @Getter private final MultiValueMap<LinkID, TaskEntry> linkTaskEntriesMap = new LinkedMultiValueMap<>();

    private TaskEntry rootEntry;
    private List<LinkID> filteredLinks;
    private TaskNodeTreeFilter filter;

    @PostConstruct
    public void init() {
        log.info("TaskEntryGridDataProvider init");
        this.rootEntry = null;
        if (settings != null) {
            this.setFilter(settings.filter());
        } else {
            this.setFilter(null);
        }
        taskNetworkGraph.taskEntryDataProvider(this);
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
                        if (entry != null && entry.node() != null) {
                            entry.node((taskNetworkGraph.nodeMap().get(entry.node().id())));
                            entry.node().child(taskNetworkGraph.taskMap().get(entry.node().child().id()));
                            this.refreshItem(entry, mapEntry.getValue());
                        }
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
        this.filteredLinks = taskNetworkGraph.getFilteredLinks(filteredLinks, filter);
    }

    public void setFilter(TaskNodeTreeFilter filter) {
        this.filter = filter;
        this.filteredLinks = taskNetworkGraph.getFilteredLinks(filteredLinks, filter);
        super.refreshAll();
        taskNetworkGraph.getNetDurations(filter);
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public int getChildCount(HierarchicalQuery<TaskEntry, Void> query) {
        log.trace("Getting child count for " + query.getParent());
        TaskID parentTaskID = query.getParent() != null ? query.getParent().task().id() : getRootTaskID();
        return taskNetworkGraph.getChildCount(parentTaskID, this.filteredLinks, query.getOffset(), query.getLimit());
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
        return taskNetworkGraph.getChildren(parentTaskID, this.filteredLinks, query.getOffset(), query.getLimit())
                .map(node -> {
                    log.trace("Fetching child: " + node);
                    TaskEntry entry = new TaskEntry(parent, taskNetworkGraph.nodeMap().get(node.id()));taskTaskEntriesMap.add(node.task().id(), entry);
                    linkTaskEntriesMap.add(node.id(), entry);
                    log.trace("Adding new entry: " + entry);
                    return entry;
                });
    }

    @Override
    public boolean hasChildren(TaskEntry item) {
        if (item == null) return true;
        boolean result = taskNetworkGraph.hasChildren(item.task().id());
        log.trace("Item {} has children: {}", item.task().name(), result);
        return result;
    }
}
