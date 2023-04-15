//package com.trajan.negentropy.grid;
//
//import com.trajan.negentropy.data.entity.old.TaskInfo;
//import com.vaadin.flow.data.provider.hierarchy.TreeData;
//
//public class NonDistinctTreeData extends TreeData<TaskInfo> {
//    // Items are hashed including the immediate parent of the instance
//    @Override
//    public TreeData<TaskInfo> addItem(TaskInfo parent, TaskInfo item) {
//        if (parent != null) parent.log("From NonDistinctTreeData - parent:");
//        TaskInfo shallowCopy = item.toBuilder().build();
//        item.setInstanceParent(parent);
//        item.log("From NonDistinctTreeData - item:");
//        return super.addItem(parent, shallowCopy);
//    }
//}
