package com.trajan.negentropy.grid;

import com.trajan.negentropy.data.entity.Task;
import com.vaadin.flow.data.provider.hierarchy.HasHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.ValueProvider;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public interface HasNonDistinctHierarchicalDataProvider extends HasHierarchicalDataProvider<Task> {
    default void setTreeData(NonDistinctTreeData treeData) {
        this.setDataProvider(new TreeDataProvider(treeData));
    }

    @Override
    default void setItems(Collection<Task> rootItems, ValueProvider<Task, Collection<Task>> childItemProvider) {
        Objects.requireNonNull(rootItems, "Given root items may not be null");
        Objects.requireNonNull(childItemProvider, "Given child item provider may not be null");
        this.setDataProvider(new TreeDataProvider<>((new NonDistinctTreeData()).addItems(rootItems, childItemProvider)));
    }

    @Override
    default void setItems(Stream<Task> rootItems, ValueProvider<Task, Stream<Task>> childItemProvider) {
        Objects.requireNonNull(rootItems, "Given root items may not be null");
        Objects.requireNonNull(childItemProvider, "Given child item provider may not be null");
        this.setDataProvider(new TreeDataProvider<>((new NonDistinctTreeData()).addItems(rootItems, childItemProvider)));
    }
}
