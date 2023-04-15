//package com.trajan.negentropy.grid;
//
//import com.trajan.negentropy.data.entity.old.TaskInfo;
//import com.vaadin.flow.component.treegrid.TreeGrid;
//import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
//
//public class NonDistinctTreeGrid extends TreeGrid<TaskInfo> implements HasNonDistinctHierarchicalDataProvider {
//    public NonDistinctTreeGrid() {
//        super();
//        setTreeData(new NonDistinctTreeData());
//    }
//
//    public NonDistinctTreeGrid(Class<TaskInfo> beanType) {
//        super(beanType);
//        setTreeData(new NonDistinctTreeData());
//    }
//
//    public NonDistinctTreeGrid(TreeDataProvider<TaskInfo> dataProvider) {
//        super(dataProvider);
//        setTreeData(new NonDistinctTreeData());
//    }
//}