package com.trajan.negentropy.grid;

import com.trajan.negentropy.data.entity.Task;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

public class NonDistinctTreeGrid extends TreeGrid<Task> implements HasNonDistinctHierarchicalDataProvider {
    public NonDistinctTreeGrid() {
        super();
        setTreeData(new NonDistinctTreeData());
    }

    public NonDistinctTreeGrid(Class<Task> beanType) {
        super(beanType);
        setTreeData(new NonDistinctTreeData());
    }

    public NonDistinctTreeGrid(TreeDataProvider<Task> dataProvider) {
        super(dataProvider);
        setTreeData(new NonDistinctTreeData());
    }
}