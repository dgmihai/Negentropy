package com.trajan.negentropy.client.components.grid;

import com.google.common.base.Joiner;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.RoutineStepFormLayout;
import com.trajan.negentropy.client.components.taskform.TaskNodeDataFormLayout;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TimeableStatusValueProvider;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

@SpringComponent
@RouteScope // TODO: Route vs UI scope?
@Scope("prototype")
@Slf4j
@Accessors(fluent = true)
@Getter
public class RoutineStepTreeGrid extends TaskTreeGrid<RoutineStep> {
    private Routine routine;

    @Autowired private TimeableStatusValueProvider timeableStatusValueProvider;

    @Override
    protected TreeGrid<RoutineStep> createGrid() {
        return new TreeGrid<>(RoutineStep.class);
    }

    @Override
    protected void setPartNameGenerator() {
        treeGrid.setPartNameGenerator(step -> {
            List<String> partNames = new ArrayList<>();

            if (step.node().completed()) {
                partNames.add(K.GRID_PARTNAME_COMPLETED);
            }
            if (step.task().block()) {
                partNames.add(K.GRID_PARTNAME_BLOCK);
            }
            if (step.task().project()) {
                partNames.add(K.GRID_PARTNAME_PROJECT);
            }

            return Joiner.on(" ").join(partNames);
        });
    }

    @Override
    protected void initAdditionalReadColumns(String column) {
        switch (column) {
            case K.COLUMN_KEY_STATUS -> treeGrid.addColumn(
                    step -> timeableStatusValueProvider.apply(step.status()))
                    .setKey(K.COLUMN_KEY_STATUS)
                    .setHeader(headerIcon(VaadinIcon.CALENDAR_CLOCK))
                    .setAutoWidth(false)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }
    }

    @Override
    protected TaskNodeDataFormLayout<RoutineStep> getTaskFormLayout(RoutineStep routineStep) {
        return new TaskNodeDataFormLayout<>(controller, routineStep, RoutineStep.class);
    }

    @Override
    protected Binder<RoutineStep> setEditorBinder(AbstractTaskFormLayout form) {
        RoutineStepFormLayout rsForm = (RoutineStepFormLayout) form;
        return rsForm.binder();
    }

    @Override
    protected Registration setEditorSaveListener() {
        return editor.addSaveListener(e -> controller.updateTask(e.getItem().task()));
    }

    @Override
    protected void configureSelectionMode() {
//        treeGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    }

    @Override
    protected void configureDragAndDrop() {
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        treeGrid.setRowsDraggable(false);
        // Drag start is managed by the dragHandleColumn

        if (visibleColumns.getOrDefault(K.COLUMN_KEY_DRAG_HANDLE, false)) {
            treeGrid.addDropListener(event -> {
                log.debug(draggedItem + " dropped onto " +
                        event.getDropTargetItem().orElseThrow().task().name());
                if (event.getDropTargetItem().isPresent()) {
                    RoutineStep target = event.getDropTargetItem().get();
                    if (!draggedItem.equals(target)) {
                        switch (event.getDropLocation()) {
                            case ABOVE -> controller.moveNodeBefore(
                                    draggedItem,
                                    target);
                            case BELOW -> controller.moveNodeAfter(
                                    draggedItem,
                                    target);
                            case ON_TOP -> controller.moveNodeInto(
                                    draggedItem,
                                    target);
                        }
                    } else {
                        NotificationError.show("Cannot move item onto itself");
                    }
                }
                draggedItem = null;
                treeGrid.setRowsDraggable(false);
            });
        }
    }

    @Override
    protected void configureAdditionalEvents() {

    }

    public void setRoutine(Routine routine) {
        if (!routine.equals(this.routine)) {
            this.routine = routine;
            TreeData<RoutineStep> treeData = new TreeData<>();
            treeData.addRootItems(routine.steps());
            treeGrid.setTreeData(treeData);
        }
    }

    public void clearRoutine() {
        this.routine = null;
        TreeData<RoutineStep> treeData = new TreeData<>();
        treeGrid.setTreeData(treeData);
    }
}
