package com.trajan.negentropy.client.routine.components;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.backend.entity.status.StepStatus;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
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
    private H2 currentTaskName;
    private StepTimer timer;
    private TextArea descriptionArea;

    private Routine routine;
    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    public RoutineCard(Routine routine, ClientDataController controller) {
        this.controller = controller;

        this.routine = routine;
        logger.debug("Routine: " + routine);
        binder.setBean(routine.steps().get(routine.currentPosition()));

        initComponents();
        arrangeComponents();
    }

    private void processStep(Function<StepID, RoutineResponse> stepFunction) {
        RoutineResponse response = stepFunction.apply(binder.getBean().id());
        this.routine = response.routine();
        binder.setBean(routine.steps().get(routine.currentPosition()));
        updateComponents();
    }

    private void updateComponents() {
        play.setVisible(binder.getBean().status().equals(StepStatus.ACTIVE));
        pause.setVisible(!play.isVisible());
        currentTaskName.setText(binder.getBean().task().name());
        prev.setEnabled(routine.currentPosition() > 0);
        timer.setStep(routine.currentStep());
    }

    private void initComponents() {
        rootTaskbar = new TaskInfoBar();

        rootTaskbar.setStep(routine.steps().get(0));

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

        currentTaskName = new H2(binder.getBean().task().name());

        timer = new StepTimer(binder.getBean(), controller);
        timer.getStyle().set("font-size", "20px");

        this.updateComponents();
        binder.addValueChangeListener(event -> updateComponents());

        descriptionArea = new TextArea();
        binder.forField(descriptionArea)
                .bindReadOnly(step -> step.task().description());
    }

    private void arrangeComponents() {
        HorizontalLayout routineUpper = new HorizontalLayout();
        HorizontalLayout routineMiddle = new HorizontalLayout();
        HorizontalLayout routineLower = new HorizontalLayout();

//        auxiliaryButtons.add(play, pause, skip);
//        auxiliaryButtons.setWidthFull();
//        auxiliaryButtons.setJustifyContentMode(JustifyContentMode.EVENLY);

        routineUpper.add(currentTaskName);
        routineUpper.setWidthFull();
        routineUpper.setJustifyContentMode(JustifyContentMode.EVENLY);

        routineMiddle.add(timer);
        routineMiddle.setWidthFull();
        routineMiddle.setJustifyContentMode(JustifyContentMode.EVENLY);

        routineLower.add(prev, skip, play, pause, next);
        routineLower.setJustifyContentMode(JustifyContentMode.EVENLY);
        routineLower.setWidthFull();

        descriptionArea.setWidthFull();

        this.add(rootTaskbar, routineUpper, routineMiddle, routineLower, descriptionArea);

        this.addClassNames(
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.SMALL);
    }

    private static class RoutineCardButton extends Div {
        public RoutineCardButton(Icon icon) {
            super(icon);

            icon.addClassNames(
                    LumoUtility.IconSize.LARGE,
                    K.COLOR_PRIMARY);
        }
    }

    private static class RoutineCardToggleableButton extends Div {
        public RoutineCardToggleableButton(Icon icon) {
            super(icon);

            icon.addClassNames(
                    LumoUtility.IconSize.LARGE);
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