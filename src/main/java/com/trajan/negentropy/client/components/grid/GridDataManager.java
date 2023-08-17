package com.trajan.negentropy.client.components.grid;//package com.trajan.negentropy.client.components.grid;
//
//import com.trajan.negentropy.client.controller.TaskNetworkGraph;
//import com.trajan.negentropy.client.controller.util.TaskEntry;
//import com.trajan.negentropy.model.Task;
//import com.trajan.negentropy.model.TaskNode;
//import com.trajan.negentropy.model.filter.TaskFilter;
//import com.trajan.negentropy.model.id.LinkID;
//import com.trajan.negentropy.model.id.TaskID;
//import com.trajan.negentropy.server.facade.QueryService;
//import com.vaadin.flow.component.treegrid.TreeGrid;
//import com.vaadin.flow.spring.annotation.SpringComponent;
//import jakarta.annotation.PreDestroy;
//import lombok.Getter;
//import lombok.Setter;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Scope;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//import java.util.LinkedList;
//import java.util.List;
//
//@SpringComponent
//@Scope("prototype")
//public class GridDataManager {
//    @Autowired private QueryService queryService;
//    @Autowired private TaskNetworkGraph taskNetworkGraph;
//
//    @Getter
//    private TaskFilter filter = null;
//    @Getter
//    private List<LinkID> filteredLinks = new LinkedList<>();
//
//    @Getter
//    @Setter
//    private TaskID rootTaskId = null;
//
//    @Getter
//    private final MultiValueMap<TaskID, TaskEntry> taskTaskEntriesMap = new LinkedMultiValueMap<>();
//    @Getter
//    private final MultiValueMap<LinkID, TaskEntry> linkTaskEntriesMap = new LinkedMultiValueMap<>();
//
//    @Getter
//    private TreeGrid<TaskEntry> treeGrid;
//
//    public void init(TreeGrid<TaskEntry> treeGrid) {
//        this.treeGrid = treeGrid;
//        taskNetworkGraph.gridDataProviders().add(this);
//    }
//
//    public GridDataManager(TreeGrid<TaskEntry> treeGrid) {
//        this.treeGrid = treeGrid;
//    }
//
//    public void setFilter(TaskFilter filter) {
//        this.filter = filter;
//        this.filteredLinks = queryService.fetchDescendantNodes(rootTaskId, filter)
//                .map(TaskNode::id)
//                .toList();
//    }
//
//    @PreDestroy
//    public void destroy() {
//        taskNetworkGraph.gridDataProviders().remove(this);
//    }
//
//    public void refresh() {
//        treeGrid.getTreeData().addItems(
//                taskNetworkGraph.getChildren(null, filteredLinks),
//                entry -> taskNetworkGraph.getChildren(entry, filteredLinks));
//    }
//
//    public void refreshItem(TaskEntry item) {
//        for (TaskEntry entry : taskTaskEntriesMap.get(item.task().id())) {
//            entry.node().child(item.task());
//            treeGrid.getDataProvider().refreshItem(entry);
//        }
//        for (TaskEntry entry : linkTaskEntriesMap.get(item.node().id())) {
//            entry.node(item.node());
//            treeGrid.getDataProvider().refreshItem(entry);
//        }
//    }
//
//    public void refreshTask(Task task) {
//        for (TaskEntry entry : taskTaskEntriesMap.get(task.id())) {
//            entry.node().child(task);
//            refreshItem(entry);
//        }
//    }
//
//    public void refreshTaskNode(TaskNode node) {
//        for (TaskEntry entry : taskTaskEntriesMap.get(node.task().id())) {
//            entry.node(node);
//            refreshItem(entry);
//        }
//        for (TaskEntry entry : linkTaskEntriesMap.get(node.node().id())) {
//            entry.node(node);
//            refreshItem(entry);
//        }
//    }
//}
