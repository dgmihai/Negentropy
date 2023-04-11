//package com.trajan.negentropy.grid;
//
//import com.trajan.negentropy.data.entity.old.Task;
//import com.vaadin.flow.data.provider.hierarchy.TreeData;
//
//public class NonDistinctTreeData extends TreeData<Task> {
//    // Items are hashed including the immediate parent of the instance
//    @Override
//    public TreeData<Task> addItem(Task parent, Task item) {
//        if (parent != null) parent.log("From NonDistinctTreeData - parent:");
//        Task shallowCopy = item.toBuilder().build();
//        item.setInstanceParent(parent);
//        item.log("From NonDistinctTreeData - item:");
//        return super.addItem(parent, shallowCopy);
//    }
//}
