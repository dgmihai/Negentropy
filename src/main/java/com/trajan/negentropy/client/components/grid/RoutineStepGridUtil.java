package com.trajan.negentropy.client.components.grid;

import com.google.common.base.Joiner;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.GridInlineEditorTaskNodeForm;
import com.trajan.negentropy.client.components.taskform.RoutineStepFormLayout;
import com.trajan.negentropy.client.components.wellness.MoodInput;
import com.trajan.negentropy.client.components.wellness.StressorInput;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.TimeableStatusValueProvider;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineNodeStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineTaskStep;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RoutineStepGridUtil {
    public static AbstractTaskFormLayout getTaskFormLayout(RoutineStep routineStep, UIController controller) {
        if (routineStep instanceof RoutineNodeStep routineNodeStep) {
            return new GridInlineEditorTaskNodeForm<>(controller, routineNodeStep, RoutineNodeStep.class);
        } else if (routineStep instanceof RoutineTaskStep routineTaskStep) {
            return new RoutineStepFormLayout(controller, routineTaskStep);
        } else {
            throw new IllegalArgumentException("Unknown routine step type: " + routineStep.getClass());
        }
    }

    public static Binder<RoutineStep> setEditorBeinder(AbstractTaskFormLayout form) {
        if (form instanceof GridInlineEditorTaskNodeForm gridInlineEditorTaskNodeForm) {
            return gridInlineEditorTaskNodeForm.binder();
        } else if (form instanceof RoutineStepFormLayout routineStepFormLayout) {
            return routineStepFormLayout.binder();
        } else {
            throw new RuntimeException("Unknown form type: " + form.getClass());
        }
    }

    public static String setPartNames(RoutineStep step, Grid<RoutineStep> grid) {
        List<String> partNames = new ArrayList<>();

        Set<TimeableStatus> grayedOut = Set.of(
                TimeableStatus.POSTPONED,
                TimeableStatus.COMPLETED,
                TimeableStatus.EXCLUDED,
                TimeableStatus.LIMIT_EXCEEDED);

        if (step.status().equals(TimeableStatus.ACTIVE)) {
            partNames.add(K.GRID_PARTNAME_PRIMARY);
        }
        if (grayedOut.contains(step.status())) {
            partNames.add(K.GRID_PARTNAME_COMPLETED);
        }
        if (step.task().required()) {
            partNames.add(K.GRID_PARTNAME_FUTURE);
        }
        if (step.task().project()) {
            partNames.add(K.GRID_PARTNAME_PROJECT);
        }

        return Joiner.on(" ").join(partNames);
    }

    public static FormLayout emotionalTrackerLayout() {
        FormLayout emotionalTrackerLayout = new FormLayout();
        emotionalTrackerLayout.add(
                SpringContext.getBean(StressorInput.class),
                SpringContext.getBean(MoodInput.class));
        emotionalTrackerLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(K.SHORT_SCREEN_WIDTH, 2));
        emotionalTrackerLayout.setWidthFull();
        return emotionalTrackerLayout;
    }

    public static void addStatusColumn(Grid<RoutineStep> grid) {
        grid.addColumn(step -> TimeableStatusValueProvider.toPresentation(step.status()))
                .setKey(ColumnKey.STATUS.toString())
                .setHeader(GridUtil.headerIcon(VaadinIcon.CALENDAR_CLOCK))
                .setAutoWidth(false)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
    }
}
