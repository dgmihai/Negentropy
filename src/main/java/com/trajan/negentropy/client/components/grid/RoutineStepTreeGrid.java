package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.GridInlineEditorTaskNodeForm;
import com.trajan.negentropy.client.components.taskform.RoutineStepFormLayout;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.RoutineDataProvider;
import com.trajan.negentropy.client.util.LimitValueProvider;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineNodeStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineTaskStep;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SpringComponent
@RouteScope // TODO: Route vs UI scope?
@Scope("prototype")
@Getter
public class
RoutineStepTreeGrid extends TaskTreeGrid<RoutineStep> {
    private final UILogger log = new UILogger();

    @Autowired private RoutineDataProvider routineDataProvider;

    private Routine routine;

    public static final List<ColumnKey> excludedColumns = List.of(
            ColumnKey.COMPLETE,
            ColumnKey.DELETE);

    public static final List<ColumnKey> possibleColumns = Arrays.stream(ColumnKey.values())
            .filter(columnKey -> !excludedColumns.contains(columnKey))
            .toList();

    @Autowired private LimitValueProvider limitValueProvider;

    @Override
    protected MultiSelectTreeGrid<RoutineStep> createGrid() {
        return new MultiSelectTreeGrid<>(RoutineStep.class);
    }

    @Override
    protected void setPartNameGenerator() {
        grid.setPartNameGenerator(step -> RoutineStepGridUtil.setPartNames(step, grid));
    }

    @Override
    protected void initAdditionalReadColumns(ColumnKey columnKey) {
        switch (columnKey) {
            case STATUS -> RoutineStepGridUtil.addStatusColumn(grid);

            case EXCLUDE -> {
                Set<TimeableStatus> grayed = Set.of(
                        TimeableStatus.NOT_STARTED,
                        TimeableStatus.SUSPENDED,
                        TimeableStatus.ACTIVE,
                        TimeableStatus.SKIPPED);

                Set<TimeableStatus> hidden = Set.of(
                        TimeableStatus.COMPLETED,
                        TimeableStatus.POSTPONED
                );
                grid.addColumn(LitRenderer.<RoutineStep>of(
                                        GridUtil.inlineVaadinIconLitExpression("minus-circle-o",
                                        "?active=\"${item.grayed}\" " +
                                                "?hidden=\"${item.hidden}\""))
                        .withFunction("onClick", step -> {
                            controller.setRoutineStepExcluded(step.id(), !step.status().equalsAny(
                                    TimeableStatus.EXCLUDED,
                                            TimeableStatus.LIMIT_EXCEEDED),
                                    response -> this.setRoutine(response.routine()),
                                    response -> NotificationMessage.error("Failed to skip/unskip routine step: "
                                            + response.message()));
                        }).withProperty("grayed", step ->
                                grayed.contains(step.status()))
                        .withProperty("hidden", step ->
                                hidden.contains(step.status())))
                        .setKey(ColumnKey.EXCLUDE.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.MINUS_CIRCLE))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);
            }

            case JUMP -> grid.addColumn(LitRenderer.<RoutineStep>of(
                                    GridUtil.inlineVaadinIconLitExpression("pointer",
                                            ""))
                            .withFunction("onClick", step ->
                                    controller.jumpToRoutineStep(step.id(),
                                            response -> this.setRoutine(response.routine()),
                                            response -> NotificationMessage.error("Failed to jump to new routine step: "
                                                    + response.message()))))
                    .setKey(ColumnKey.JUMP.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.AREA_SELECT))
                    .setWidth(GridUtil.ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case LIMIT -> configureLimitColumn(grid.addColumn(
                            step -> (step.nodeOptional().isPresent())
                                    ? limitValueProvider.apply(step.node().limits())
                                    : ""));
        }
    }

    @Override
    protected List<ColumnKey> getPossibleColumns() {
        return possibleColumns;
    }

    @Override
    protected AbstractTaskFormLayout getTaskFormLayout(RoutineStep routineStep) {
        if (routineStep instanceof RoutineNodeStep routineNodeStep) {
            return new GridInlineEditorTaskNodeForm<>(controller, routineNodeStep, RoutineNodeStep.class);
        } else if (routineStep instanceof RoutineTaskStep routineTaskStep) {
            return new RoutineStepFormLayout(controller, routineTaskStep);
        } else {
            throw new IllegalArgumentException("Unknown routine step type: " + routineStep.getClass());
        }
    }

    @Override
    protected Binder<RoutineStep> setEditorBinder(AbstractTaskFormLayout form) {
        return RoutineStepGridUtil.setEditorBeinder(form);
    }

    @Override
    protected Registration setEditorSaveListener() {
        return editor.addSaveListener(e ->
                controller.requestChangeAsync(new MergeChange<>(e.getItem().task())));
    }

    @Override
    protected void configureDragAndDrop() {
        grid.setDropMode(GridDropMode.BETWEEN);
        grid.setRowsDraggable(false);
        // Drag start is managed by the dragHandleColumn

        if (visibleColumns.getOrDefault(ColumnKey.DRAG_HANDLE, false)) {
            grid.addDropListener(event -> {
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
                                grid.getDataProvider().refreshAll();
                            }
                            case BELOW -> {
                                this.routine = controller.moveRoutineStep(
                                        InsertLocation.AFTER,
                                        draggedItem,
                                        target).routine();
                                grid.getDataProvider().refreshAll();
                            }
                        }
                    } else {
                        NotificationMessage.error("Cannot move item onto itself");
                    }
                }
                draggedItem = null;
                grid.setRowsDraggable(false);
            });
        }
    }

    @Override
    protected void configureAdditionalEvents() {
    }

    @Override
    protected void onManualGridRefresh() {
    }

    public void setRoutine(Routine routine) {
        log.debug("Setting routine: " + routine.name() + " with " + routine.countSteps() + " steps.");
        this.routine = routine;

        if (settings.hideFinishedRoutineSteps()) {
            List<RoutineStep> rootItems = routine.children().stream()
                    .filter(step -> !step.status().isFinishedOrExceeded())
                    .toList();
            grid().setItems(rootItems, s -> s.children().stream()
                    .filter(step -> !step.status().isFinishedOrExceeded())
                    .toList());
        } else {
            grid().setItems(routine.children(), RoutineStep::children);
        }

        grid().addExpandListener(e -> {
            for (RoutineStep child : e.getItems().stream()
                    .flatMap(
                    step -> step.children().stream())
                    .toList()) {
                if (child.status().equalsAny(
                        TimeableStatus.ACTIVE,
                        TimeableStatus.SUSPENDED,
                        TimeableStatus.DESCENDANT_ACTIVE)) {
                    grid().expand(child);
                }
            }
        });
        grid().expand(routine.children());
    }

    public void setRoutine(RoutineID routineId) {
        this.setRoutine(controller.services().routine().fetchRoutine(routineId));
    }

    public void clearRoutine() {
        this.routine = null;
        TreeData<RoutineStep> treeData = new TreeData<>();
        grid().setTreeData(treeData);
    }

    @Override
    public Optional<TaskNode> rootNode() {
        return Optional.of(routine.children().get(0).node());
    }
}
