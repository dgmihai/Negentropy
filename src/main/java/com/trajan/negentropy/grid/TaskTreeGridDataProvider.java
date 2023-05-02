//package com.trajan.negentropy.grid;
//
//import com.trajan.negentropy.server.facade.TaskQueryService;
//import com.trajan.negentropy.server.facade.model.Task;
//import com.trajan.negentropy.server.facade.model.TaskLink;
//import com.trajan.negentropy.server.facade.model.TaskNode;
//import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
//import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
//import com.vaadin.flow.function.SerializablePredicate;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Stream;
//
//public class TaskTreeGridDataProvider extends AbstractBackEndHierarchicalDataProvider<TaskNode, SerializablePredicate<TaskNode>> {
//
//    private final TaskQueryService taskService;
//
//    @Autowired
//    public TaskTreeGridDataProvider(TaskQueryService taskService) {
//        this.taskService = taskService;
//    }
//
//    @Override
//    public int getChildCount(HierarchicalQuery<TaskNode, SerializablePredicate<TaskNode>> query) {
//        TaskNode parent = query.getParentOptional().orElse(null);
//
//        return (parent == null) ?
//            taskService.countRoots() :
//            taskService.countChildren(parent.task().id());
//    }
//
//    // The root level Tasks.
//    @Override
//    public Stream<TaskNode> fetchChildrenFromBackEnd(HierarchicalQuery<TaskNode, SerializablePredicate<TaskNode>> query) {
//        TaskNode parent = query.getParentOptional().orElse(null);
//        if (parent == null) {
//            return taskService.getRootTasks()
//                    .map(task -> new TaskNode(UUID.randomUUID(), task));
//        }
//
//        List<TaskLink> childTaskLinks = taskService.findChildTaskLinks(parent.task().id());
//        return childTaskLinks.stream().map(taskLink -> {
//            Task childTask = taskService.findTaskById(taskLink.childId());
//            return new TaskNode(UUID.randomUUID(), childTask, taskLink);
//        });
//    }
//
//    // Check if a TaskNode has children.
//    @Override
//    public boolean hasChildren(TaskNode item) {
//        return taskService.hasChildTasks(item.task().id());
//    }
//
//    // Refresh the TaskNodes with the updated Task information.
//    public void refreshTask(Task updatedTask) {
//        getTreeData().getItems().stream()
//                .filter(taskNode -> taskNode.task().id().equals(updatedTask.id()))
//                .forEach(taskNode -> {
//                    TaskNode updatedTaskNode = new TaskNode(taskNode.uuid(), updatedTask, taskNode.taskLink());
//                    getTreeData().updateItem(taskNode, updatedTaskNode);
//                });
//    }
//
//    // Refresh the TaskNodes with the updated TaskLink information.
//    public void refreshTaskLink(TaskLink updatedTaskLink) {
//        getTreeData().getItems().stream()
//                .filter(taskNode -> taskNode.taskLink().id().equals(updatedTaskLink.id()))
//                .forEach(taskNode -> {
//                    TaskNode updatedTaskNode = new TaskNode(taskNode.uuid(), taskNode.task(), updatedTaskLink);
//                    getTreeData().updateItem(taskNode, updatedTaskNode);
//                });
//    }
//}
