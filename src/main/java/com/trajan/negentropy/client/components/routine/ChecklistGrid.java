package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.components.grid.AbstractTaskGrid;
import com.trajan.negentropy.client.components.grid.GridUtil;
import com.trajan.negentropy.client.components.grid.RoutineStepGridUtil;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.HasRootNode;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@SpringComponent
@RouteScope
@Scope("prototype")
@Getter
public class ChecklistGrid extends AbstractTaskGrid<RoutineStep> implements HasRootNode {
    private final UILogger log = new UILogger();

    @Autowired protected UIController controller;
    @Autowired protected SessionServices services;

    private Registration routineListener = null;
    private Routine routine = null;
    private RoutineStep rootStep = null;

    public static final List<ColumnKey> possibleColumns = List.of(
            ColumnKey.PLAY,
            ColumnKey.DESCRIPTION,
            ColumnKey.NAME,
            ColumnKey.COMPLETE,
            ColumnKey.EXCLUDE,
            ColumnKey.POSTPONE,
            ColumnKey.EDIT);

    public void init() {
        LinkedHashMap<ColumnKey, Boolean> visibleColumns = new LinkedHashMap<>();
        possibleColumns.forEach(columnKey -> visibleColumns.put(columnKey, true));
        init(visibleColumns, SelectionMode.SINGLE);
        addDetachListener(e -> destroy());
    }

    @PreDestroy
    public void destroy() {
        if (routineListener != null) {
            routineListener.remove();
        }
    }

    @Override
    protected Grid<RoutineStep> createGrid() {
        return new Grid<>(RoutineStep.class);
    }

    @Override
    protected void setSelectionMode(SelectionMode selectionMode) {
        grid.setSelectionMode(selectionMode);
        grid.addSelectionListener(e -> {
            if (e.isFromClient()) {
                grid.deselectAll();
            }
        });
    }

    @Override
    protected void initAdditionalReadColumns(ColumnKey columnKey) {
        switch (columnKey) {
            case STATUS -> RoutineStepGridUtil.addStatusColumn(grid);
            case PLAY -> grid.addColumn(LitRenderer.<RoutineStep>of(
                    GridUtil.inlineVaadinIconLitExpression("play-circle",
                            "?active=\"${item.isActive}\""))
                    .withFunction("onClick", s -> {
                        if (s.status().equals(TimeableStatus.ACTIVE)) {
                            controller.pauseRoutineStep(s.id(),
                                    null,
                                    r -> NotificationMessage.error("Failed to pause routine: " + r.message()));
                        } else {
                            controller.jumpToRoutineStep(s.id(),
                                    null,
                                    r -> NotificationMessage.error("Failed to activate routine: " + r.message()));
                        }
                    })
                    .withProperty("isActive", s ->
                            s.status().equals(TimeableStatus.ACTIVE)))
                    .setKey(ColumnKey.PLAY.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.PLAY_CIRCLE))
                    .setWidth(GridUtil.ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER)
                    .setTooltipGenerator(t -> ColumnKey.PLAY.toString())
                    .setClassName("play-column");
            case COMPLETE -> grid.addColumn(LitRenderer.<RoutineStep>of(
                    GridUtil.inlineVaadinIconLitExpression("check",
                            " active"))
                    .withFunction("onClick", s -> {
                        Consumer<Response> complete = res -> controller.completeRoutineStep(s.id(),
                                null,
                                r -> NotificationMessage.error("Failed to complete routine: " + r.message()));
                        if (!routine.currentStep().equals(s)) {
                            controller.pauseRoutineStep(routine.currentStep().id(),
                                    complete::accept,
                                    r -> NotificationMessage.error("Failed to skip routine: " + r.message()));
                        } else {
                            complete.accept(null);
                        }
                    }))
                    .setKey(ColumnKey.COMPLETE.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.CHECK_SQUARE_O))
                    .setWidth(GridUtil.ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER)
                    .setTooltipGenerator(e -> ColumnKey.COMPLETE.toString());
            case EXCLUDE -> grid.addColumn(LitRenderer.<RoutineStep>of(
                    GridUtil.inlineVaadinIconLitExpression("minus-circle-o",
                            " active"))
                    .withFunction("onClick", step ->
                            controller.excludeRoutineStep(step.id(),
                                    null,
                                    r -> NotificationMessage.error("Failed to exclude routine: " + r.message()))))
                    .setKey(ColumnKey.EXCLUDE.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.MINUS_CIRCLE_O))
                    .setWidth(GridUtil.ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER)
                    .setTooltipGenerator(e -> ColumnKey.EXCLUDE.toString());
            case POSTPONE -> grid.addColumn(LitRenderer.<RoutineStep>of(
                    GridUtil.inlineVaadinIconLitExpression("time-forward",
                            " active"))
                    .withFunction("onClick", step ->
                            controller.postponeRoutineStep(step.id(),
                                    null,
                                    r -> NotificationMessage.error("Failed to postpone routine: " + r.message()))))
                    .setKey(ColumnKey.POSTPONE.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.TIME_FORWARD))
                    .setWidth(GridUtil.ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER)
                    .setTooltipGenerator(e -> ColumnKey.POSTPONE.toString());
        }
    }

    @Override
    protected void setPartNameGenerator() {
        grid.setPartNameGenerator(step -> RoutineStepGridUtil.setPartNames(step, grid));
    }

    @Override
    protected List<ColumnKey> getPossibleColumns() {
        return possibleColumns;
    }

    @Override
    protected AbstractTaskFormLayout getTaskFormLayout(RoutineStep routineStep) {
        return RoutineStepGridUtil.getTaskFormLayout(routineStep, controller);
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
    public Optional<TaskNode> rootNode() {
        return Optional.of(rootStep.node());
    }

    private List<RoutineStep> filterFinalizedSteps(List<RoutineStep> steps) {
        return steps.stream()
                .filter(step -> !step.status().equalsAny(
                        TimeableStatus.COMPLETED,
                        TimeableStatus.EXCLUDED,
                        TimeableStatus.POSTPONED,
                        TimeableStatus.LIMIT_EXCEEDED))
                .toList();
    }

    public synchronized void setRoutine(Routine routine) {
        if (routineListener != null) {
            routineListener.remove();
        }

        this.routine = routine;
        routineListener = controller.registerRoutineListener(routine.id(), this::setRoutine);
        RoutineStep currentStep = routine.currentStep();
        List<RoutineStep> items = filterFinalizedSteps(routine.currentStep().children());
        if (items.isEmpty()) {
            this.rootStep = routine.steps().get(currentStep.parentId());
            items = filterFinalizedSteps(rootStep.children());
        }
        grid.setItems(items);
        List<RoutineStep> activeSteps = items.stream()
                .filter(step -> step.status().equalsAny(
                        TimeableStatus.ACTIVE,
                        TimeableStatus.SUSPENDED))
                .toList();
        if (activeSteps.size() > 1) {
            String err = "Multiple active steps found for routine: " + routine.name();
            NotificationMessage.error(err);
            log.error(err);
        } else if (!activeSteps.isEmpty()) {
            RoutineStep step = activeSteps.get(0);
            grid.select(step);
            grid.setDetailsVisible(step, !step.description().isBlank());
        }
    }
}
