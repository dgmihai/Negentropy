package com.trajan.negentropy.client.components.grid;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.grid.AbstractGridMultiSelectionModel;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.selection.SelectionEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MultiSelectTreeGrid<T> extends TreeGrid<T> {
    public MultiSelectTreeGrid(Class<T> beanType) {
        super(beanType);
    }

    @Override
    public GridSelectionModel<T> setSelectionMode(SelectionMode selectionMode) {
        if (SelectionMode.MULTI == selectionMode) {
            GridMultiSelectionModel<T> model = new AbstractGridMultiSelectionModel<T>(this) {
                @SuppressWarnings("unchecked")
                @Override
                protected void fireSelectionEvent(
                        SelectionEvent<Grid<T>, T> event) {
                    fireEvent((ComponentEvent<Grid<?>>) event);
                }
            };
//                @Override
//                protected void fireSelectionEvent(SelectionEvent<Grid<T>, T> event) {
//
//                }
//                @Override
//                protected void fireSelectionEvent(SelectionEvent<Grid<T>, T> event) {
//                    ((RecursiveSelectTreeGrid<T>) this.getGrid()).fireEvent((ComponentEvent<Grid<?>>) event);
//                }
//
//                @Override
//                public void selectFromClient(T item) {
//                    updateSelection(new HashSet<>(getChildrenRecursively(Collections.singletonList(item), 99)),
//                            Collections.emptySet());
//                }
//
//                @Override
//                public void deselectFromClient(T item) {
//                    updateSelection(Collections.emptySet(), new HashSet<>(getChildrenRecursively(Collections.singletonList(item), 99)));
//                }

//            };
            model.setSelectionColumnFrozen(true);
            setSelectionModel(model, selectionMode);
            return model;
        } else {
            return super.setSelectionMode(selectionMode);
        }
    }

    protected Collection<T> getChildrenRecursively(Collection<T> items,
                                                   int depth) {
        List<T> itemsWithChildren = new ArrayList<>();
        if (depth < 0) {
            return itemsWithChildren;
        }
        items
                .forEach(item -> {
                    itemsWithChildren.add(item);
                    if (getDataCommunicator().hasChildren(item)) {
                        itemsWithChildren.addAll(
                                getChildrenRecursively(getDataProvider()
                                        .fetchChildren(
                                                new HierarchicalQuery<>(null, item))
                                        .collect(Collectors.toList()), depth - 1));
                    }
                });
        return itemsWithChildren;
    }
}
