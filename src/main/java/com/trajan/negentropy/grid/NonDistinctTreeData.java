//package com.trajan.negentropy.grid;
//
//import com.trajan.negentropy.dataTaskId.entity.old.Task;
//import com.trajan.negentropy.server.facade.model.Task;
//import com.vaadin.flow.data.provider.hierarchy.TreeData;
//import com.vaadin.flow.dataTaskId.provider.hierarchy.TreeData;
//
//public class NonDistinctTreeData extends TreeData<Task> {
//    // Items are hashed including the immediate parent of the instance
//    @Override
//    public TreeData<Task> addItem(Task parent, Task item) {
//        Task shallowCopy = item.toBuilder().build();
//        item.setInstanceParent(parent);
//        item.log("From NonDistinctTreeData - item:");
//        return super.addItem(parent, shallowCopy);
//    }
//}
