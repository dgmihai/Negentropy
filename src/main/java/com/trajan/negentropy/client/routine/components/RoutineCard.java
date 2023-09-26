package com.trajan.negentropy.client.routine.components;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.Function;

@Slf4j
public class RoutineCard extends VerticalLayout {
    private final UIController controller;

    private TaskInfoBar rootTaskbar;
//    private TaskInfoBar parentTaskbar;
    private RoutineCardButton next;
    private RoutineCardToggleableButton prev;
    private RoutineCardButton pause;
    private RoutineCardButton play;
    private RoutineCardButton skip;
    private RoutineCardToggleableButton recalculate;
    private RoutineCardButton postpone;
    private RoutineCardButton close;
    private Span currentTaskName;
    private RoutineTimer timer;
    private TextArea description;

    private Routine routine;
    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    private RoutineStepTreeGrid routineStepTreeGrid;

    public RoutineCard(Routine routine, UIController controller, RoutineStepTreeGrid routineStepTreeGrid) {
        this.controller = controller;
        this.routineStepTreeGrid = routineStepTreeGrid;

        this.routine = routine;
        log.debug("Routine card created for routine: " + routine.name() + " with " + routine.countSteps() + " steps.");

        if (routine.status().equals(TimeableStatus.COMPLETED)) {
            this.setCardCompleted();
        } else {
            binder.setBean(routine.currentStep());
            initComponents();
        }
        layout();
    }

    private void processStep(Function<StepID, RoutineResponse> stepFunction) {
        RoutineResponse response = stepFunction.apply(binder.getBean().id());
        if (response.success()) {
            this.routine = response.routine();
            this.updateComponents();
        }
    }

    private void processRoutine(Function<RoutineID, RoutineResponse> stepFunction) {
        log.debug("Processing routine: " + binder.getBean().routineId());
        RoutineResponse response = stepFunction.apply(binder.getBean().routineId());
        if (response.success()) {
            this.routine = response.routine();
            this.updateComponents();
        }
    }

    private boolean isActive() {
        return binder.getBean().status().equals(TimeableStatus.ACTIVE);
    }

    private void updateComponents() {
        if (routine.status().equals(TimeableStatus.COMPLETED)) {
            this.setCardCompleted();
        } else if (routine.status().equals(TimeableStatus.SKIPPED)) {
            this.setCardSkipped();
        } else {
            binder.setBean(routine.currentStep());
            this.getElement().setAttribute("active", isActive());
            play.setVisible(!isActive());
            pause.setVisible(isActive());
            prev.setEnabled(routine.currentPosition() > 0);
            timer.setTimeable(binder.getBean());
            recalculate.setEnabled(!controller.taskNetworkGraph().syncId().equals(routine.syncId()));
            rootTaskbar.setTimeable(routine);
            rootTaskbar.timer().run(isActive());
        }
        if (routineStepTreeGrid.routine() != null) {
            routineStepTreeGrid.setRoutine(routine);
        }
    }

    private void setCardCompleted() {
        this.removeAll();
        this.getElement().setAttribute("active", isActive());

        Span completed;
        if (routine.rootStep().startTime() == null) {
            NotificationMessage.error("Start at least one step in a routine before completing!");
            completed = new Span("Completed '" + routine.name() + "!");
        } else {
            completed = new Span("Completed '" + routine.name() + "' in " +
                    DurationConverter.toPresentation(Duration.between(
                            routine.rootStep().startTime(),
                            routine.finishTime()))
                    + "!");
        }

        completed.addClassName("name");
        this.add(completed);
    }

    private void setCardSkipped() {
        this.removeAll();
        this.getElement().setAttribute("active", isActive());

        Span skipped = new Span("Skipped '" + routine.name() + "'.");

        skipped.addClassName("name");
        this.add(skipped);
    }

    private void initComponents() {
        rootTaskbar = new TaskInfoBar(routine, controller);

        next = new RoutineCardButton(VaadinIcon.CHEVRON_RIGHT.create());
        next.addClickListener(event -> this.processStep(
                controller::completeRoutineStep));

        prev = new RoutineCardToggleableButton(VaadinIcon.CHEVRON_LEFT.create());
        prev.addClickListener(event -> this.processStep(
                controller::previousRoutineStep));

        play = new RoutineCardButton(VaadinIcon.PLAY.create());
        play.addClickListener(event -> this.processStep(
                controller::startRoutineStep));

        pause = new RoutineCardButton(VaadinIcon.PAUSE.create());
        pause.addClickListener(event -> this.processStep(
                controller::pauseRoutineStep));

        recalculate = new RoutineCardToggleableButton(VaadinIcon.REFRESH.create());
        recalculate.addClickListener(event -> this.processRoutine(
                controller::recalculateRoutine));

        skip = new RoutineCardButton(VaadinIcon.STEP_FORWARD.create());
        skip.addClickListener(event -> this.processStep(
                controller::skipRoutineStep));

        postpone = new RoutineCardButton(VaadinIcon.TIME_FORWARD.create());
        postpone.addClickListener(event -> this.processStep(
                controller::postponeRoutineStep));

        close = new RoutineCardButton(VaadinIcon.CLOSE.create());
        close.addClickListener(event -> this.processRoutine(
                controller::skipRoutine));

        currentTaskName = new Span(binder.getBean().task().name());
        ReadOnlyHasValue<String> taskName = new ReadOnlyHasValue<>(
                text -> currentTaskName.setText(text));
        binder.forField(taskName)
                .bindReadOnly(step -> step.task().name());

        timer = new RoutineTimer(binder.getBean(), controller);

        this.updateComponents();

        description = new TextArea();
        binder.forField(description)
                .bind(step -> step.task().description(),
                        (step, text) -> step.task().description(text));
        description.setValueChangeMode(ValueChangeMode.LAZY);
        description.addValueChangeListener(event -> {
            Change change = Change.update(binder.getBean().task());
            DataMapResponse response = controller.requestChange(change);
            if (response.success()) {
                binder.getBean().task(((Task) response.changeRelevantDataMap().getFirst(change.id())));
            }
        });
    }

    private void layout() {
        this.setSpacing(false);
        this.addClassName("card");

        rootTaskbar.setPadding(false);

        HorizontalLayout lower = new HorizontalLayout();
        lower.addClassName("header");
        lower.setWidthFull();

        lower.getThemeList().add("spacing-s");

        Div left = new Div(prev);
        left.setHeightFull();
        left.addClassName("card-side");

        Div right = new Div(next);
        right.setHeightFull();
        right.addClassName("card-side");

        currentTaskName.addClassName("name");
        timer.addClassName("name");

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setSpacing(false);
        header.getThemeList().add("spacing-s");
        header.setWidthFull();
        header.add(currentTaskName, timer);

        HorizontalLayout auxActions = new HorizontalLayout();
        auxActions.addClassName("footer");
        auxActions.setWidthFull();
        auxActions.add(close, recalculate, play, pause, skip, postpone);

        VerticalLayout middle = new VerticalLayout();
        middle.addClassName("middle");
        middle.setSpacing(false);
        middle.setPadding(false);
        middle.add(description, auxActions);

        description.setWidthFull();

        lower.add(left, middle, right);
        this.add(rootTaskbar, header, lower);
        this.setWidthFull();

        header.getElement().addEventListener("click", e -> {
            if (isActive()) {
                processStep(controller::pauseRoutineStep);
            } else {
                processStep(controller::startRoutineStep);
            }
        });
    }

    private static class RoutineCardButton extends Div {
        public RoutineCardButton(Icon icon) {
            super(icon);

            icon.addClassNames(
                    LumoUtility.IconSize.MEDIUM,
                    K.COLOR_PRIMARY);
        }
    }

    private static class RoutineCardToggleableButton extends Div {
        public RoutineCardToggleableButton(Icon icon) {
            super(icon);

            icon.addClassNames(
                    LumoUtility.IconSize.MEDIUM);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);

            if (enabled) {
                this.addClassName(K.COLOR_PRIMARY);
                this.removeClassName(K.COLOR_UNSELECTED);
            } else {
                this.removeClassName(K.COLOR_PRIMARY);
                this.addClassName(K.COLOR_UNSELECTED);
            }
        }
    }
}