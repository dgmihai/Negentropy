package com.trajan.negentropy.client.components.grid;

import com.google.common.base.Joiner;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.RoutineStepFormLayout;
import com.trajan.negentropy.client.components.taskform.TaskNodeDataFormLayout;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TimeableStatusValueProvider;
import com.trajan.negentropy.model.Routine;
import com.trajan.negentropy.model.RoutineStep;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.*;

@SpringComponent
@RouteScope // TODO: Route vs UI scope?
@Scope("prototype")
@Slf4j
@Accessors(fluent = true)
@Getter
public class RoutineStepTreeGrid extends TaskTreeGrid<RoutineStep> {
    private Routine routine;

    public static final List<ColumnKey> excludedColumns = List.of(
            ColumnKey.COMPLETE,
            ColumnKey.DELETE,
            ColumnKey.EDIT);

    public static final List<ColumnKey> possibleColumns = Arrays.stream(ColumnKey.values())
            .filter(columnKey -> !excludedColumns.contains(columnKey))
            .toList();

    @Autowired private TimeableStatusValueProvider timeableStatusValueProvider;

    @Override
    protected TreeGrid<RoutineStep> createGrid() {
        return new TreeGrid<>(RoutineStep.class);
    }

    @Override
    protected void setPartNameGenerator() {
        treeGrid.setPartNameGenerator(step -> {
            List<String> partNames = new ArrayList<>();

            Set<TimeableStatus> grayedOut = Set.of(
                    TimeableStatus.SKIPPED,
                    TimeableStatus.COMPLETED,
                    TimeableStatus.EXCLUDED);

            if (step.status().equals(TimeableStatus.ACTIVE)) {
                partNames.add(K.GRID_PARTNAME_RECURRING); // TODO: Rename from recurring
            }
            if (grayedOut.contains(step.status())) {
                partNames.add(K.GRID_PARTNAME_COMPLETED);
            }
            if (step.task().required()) {
                partNames.add(K.GRID_PARTNAME_REQUIRED);
            }
            if (step.task().project()) {
                partNames.add(K.GRID_PARTNAME_PROJECT);
            }

            return Joiner.on(" ").join(partNames);
        });
    }

    @Override
    protected void initAdditionalReadColumns(ColumnKey columnKey) {
        switch (columnKey) {
            case STATUS -> treeGrid.addColumn(
                    step -> timeableStatusValueProvider.apply(step.status()))
                    .setKey(ColumnKey.STATUS.toString())
                    .setHeader(headerIcon(VaadinIcon.CALENDAR_CLOCK))
                    .setAutoWidth(false)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case EXCLUDE -> {
                Set<TimeableStatus> excludable = Set.of(
                        TimeableStatus.NOT_STARTED,
                        TimeableStatus.SUSPENDED,
                        TimeableStatus.ACTIVE);

                Set<TimeableStatus> hidden = Set.of(
                        TimeableStatus.COMPLETED
                );
                treeGrid.addColumn(LitRenderer.<RoutineStep>of(
                                inlineVaadinIconLitExpression("minus-circle-o",
                                        "?active=\"${item.excludable}\" " +
                                                "?hidden=\"${item.hidden}\""))
                        .withFunction("onClick", step -> {
                            RoutineResponse response = controller.setRoutineStepExcluded(
                                    step.id(),
                                    excludable.contains(step.status()));
                            if (response.success()) {
                                this.setRoutine(response.routine());
                            } else {
                                NotificationError.show("Failed to skip/unskip routine step: " + response.message());
                            }
                        })
                        .withProperty("excludable", step ->
                                excludable.contains(step.status()))
                        .withProperty("hidden", step ->
                                hidden.contains(step.status())))
                        .setKey(ColumnKey.EXCLUDE.toString())
                        .setHeader(headerIcon(VaadinIcon.MINUS_CIRCLE))
                        .setWidth(ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);
            }

            case GOTO -> treeGrid.addColumn(LitRenderer.<RoutineStep>of(
                                    inlineVaadinIconLitExpression("crosshairs",
                                            ""))
                            .withFunction("onClick", step -> {
                                controller.pauseRoutineStep(routine.currentStep().id());
                                RoutineResponse response = controller.startRoutineStep(step.id());
                                if (response.success()) {
                                    this.setRoutine(response.routine());
                                } else {
                                    NotificationError.show("Failed to set current routine step: " + response.message());
                                }
                            })
                    )
                    .setKey(ColumnKey.GOTO.toString())
                    .setHeader(headerIcon(VaadinIcon.CROSSHAIRS))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }
    }

    @Override
    protected List<ColumnKey> getPossibleColumns() {
        return possibleColumns;
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
        return editor.addSaveListener(e ->
                controller.requestChange(Change.merge(e.getItem().task())));
    }

    @Override
    protected void toggleSelectionMode() {
        treeGrid.setSelectionMode(Grid.SelectionMode.NONE);
    }

    @Override
    protected void configureDragAndDrop() {
        treeGrid.setDropMode(GridDropMode.BETWEEN);
        treeGrid.setRowsDraggable(false);
        // Drag start is managed by the dragHandleColumn

        if (visibleColumns.getOrDefault(ColumnKey.DRAG_HANDLE, false)) {
            treeGrid.addDropListener(event -> {
                log.debug(draggedItem + " dropped onto " +
                        event.getDropTargetItem().orElseThrow().task().name());
                if (event.getDropTargetItem().isPresent()) {
                    RoutineStep target = event.getDropTargetItem().get();
                    if (!draggedItem.equals(target)) {
                        switch (event.getDropLocation()) {
                            case ABOVE -> {
                                this.routine = controller.moveRoutineStep(
                                        InsertLocation.BEFORE,
                                        draggedItem,
                                        target).routine();
                                treeGrid.getDataProvider().refreshAll();
                            }
                            case BELOW -> {
                                this.routine = controller.moveRoutineStep(
                                        InsertLocation.AFTER,
                                        draggedItem,
                                        target).routine();
                                treeGrid.getDataProvider().refreshAll();
                            }
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

    @Override
    public Optional<LinkID> rootNodeId() {
        return Optional.of(routine.rootStep().node().id());
    }
}
