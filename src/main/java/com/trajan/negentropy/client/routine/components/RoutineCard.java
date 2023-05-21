package com.trajan.negentropy.client.routine.components;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.server.facade.response.TaskResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class RoutineCard extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(RoutineCard.class);

    private final ClientDataController controller;

    private TaskInfoBar rootTaskbar;
//    private TaskInfoBar parentTaskbar;
    private RoutineCardButton next;
    private RoutineCardToggleableButton prev;
    private RoutineCardButton pause;
    private RoutineCardButton play;
    private RoutineCardButton skip;
    private Span currentTaskName;
    private RoutineTimer timer;
    private TextArea description;

    private Routine routine;
    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    public RoutineCard(Routine routine, ClientDataController controller) {
        this.controller = controller;

        this.routine = routine;
        logger.debug("Routine: " + routine);
        binder.setBean(routine.steps().get(routine.currentPosition()));

        initComponents();
        layout();
    }

    private void processStep(Function<StepID, RoutineResponse> stepFunction) {
        RoutineResponse response = stepFunction.apply(binder.getBean().id());
        this.routine = response.routine();
        binder.setBean(routine.steps().get(routine.currentPosition()));
        updateComponents();
    }

    private boolean isActive() {
        return binder.getBean().status().equals(TimeableStatus.ACTIVE);
    }

    private void updateComponents() {
        this.getElement().setAttribute("active", isActive());
        prev.setEnabled(routine.currentPosition() > 0);
        timer.setTimeable(binder.getBean());
        rootTaskbar.timer().run(!isActive());
    }

    private void initComponents() {
        rootTaskbar = new TaskInfoBar(routine.rootStep(), controller);

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

        skip = new RoutineCardButton(VaadinIcon.CLOSE.create());
        skip.addClickListener(event -> this.processStep(
                controller::skipRoutineStep));

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
            TaskResponse response = controller.updateTask(binder.getBean().task());
            if (response.success()) {
                binder.getBean().task(response.task());
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

        VerticalLayout middle = new VerticalLayout();
        middle.addClassName("middle");
        middle.setSpacing(false);
        middle.setPadding(false);
        middle.add(header, description);

        HorizontalLayout routineMiddle = new HorizontalLayout();
        HorizontalLayout routineLower = new HorizontalLayout();

        HorizontalLayout upper = new HorizontalLayout();
        upper.setWidthFull();
        upper.setJustifyContentMode(JustifyContentMode.EVENLY);

        routineMiddle.setWidthFull();
        routineMiddle.setJustifyContentMode(JustifyContentMode.EVENLY);

        routineLower.setWidthFull();

        description.setWidthFull();

        lower.add(left, middle, right);
        this.add(rootTaskbar, lower);
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