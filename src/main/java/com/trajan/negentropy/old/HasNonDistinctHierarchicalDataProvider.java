//package com.trajan.negentropy.grid;
//
//import com.trajan.negentropy.data.entity.old.TaskInfo;
//import com.vaadin.flow.data.provider.hierarchy.HasHierarchicalDataProvider;
//import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
//import com.vaadin.flow.function.ValueProvider;
//
//import java.util.Collection;
//import java.util.Objects;
//import java.util.stream.Stream;
//
//public interface HasNonDistinctHierarchicalDataProvider extends HasHierarchicalDataProvider<TaskInfo> {
//    default void setTreeData(NonDistinctTreeData treeData) {
//        this.setDataProvider(new TreeDataProvider(treeData));
//    }
//
//    @Override
//    default void setItems(Collection<TaskInfo> rootItems, ValueProvider<TaskInfo, Collection<TaskInfo>> childItemProvider) {
//        Objects.requireNonNull(rootItems, "Given root items may not be null");
//        Objects.requireNonNull(childItemProvider, "Given child item provider may not be null");
//        this.setDataProvider(new TreeDataProvider<>((new NonDistinctTreeData()).addItems(rootItems, childItemProvider)));
//    }
//
//    @Override
//    default void setItems(Stream<TaskInfo> rootItems, ValueProvider<TaskInfo, Stream<TaskInfo>> childItemProvider) {
//        Objects.requireNonNull(rootItems, "Given root items may not be null");
//        Objects.requireNonNull(childItemProvider, "Given child item provider may not be null");
//        this.setDataProvider(new TreeDataProvider<>((new NonDistinctTreeData()).addItems(rootItems, childItemProvider)));
//    }
//}
