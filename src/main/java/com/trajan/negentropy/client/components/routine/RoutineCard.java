package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.TreeView;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RoutineCard extends VerticalLayout {
    private final UILogger log = new UILogger();

    @Getter private final UIController controller;

    private List<Component> nestedTaskInfoBars;
    private RoutineCardButton next;
    private RoutineCardToggleableButton prev;
    private RoutineCardButton pause;
    private RoutineCardButton play;
    private RoutineCardButton skip;
    private RoutineCardBooleanButton recalculate;
    private RoutineCardButton postpone;
    private RoutineCardButton close;
    private RoutineCardButton toTaskTree;
    private Span currentTaskName;
    private CountdownTimer timer;
    private TextArea description;

    private VerticalLayout taskInfoBarLayout;

    private Routine routine;
    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    private RoutineStepTreeGrid routineStepTreeGrid;

    public RoutineCard(Routine routine, UIController controller, RoutineStepTreeGrid routineStepTreeGrid) {
        this.controller = controller;
        this.routineStepTreeGrid = routineStepTreeGrid;

        this.routine = routine;
        log.debug("Routine card created for routine  <" + routine.name() + "> with " + routine.countSteps() + " steps.");

        if (routine.status().equals(TimeableStatus.COMPLETED)) {
            this.setCardCompleted();
        } else {
            binder.setBean(routine.currentStep());
            initComponents();
        }

        layout();

        controller.registerRoutineCard(routine.id(), r -> {
            log.debug("Updated routine: " + r);
            updateRoutine(r);
        });
    }

    public void processStep(Function<StepID, RoutineResponse> stepFunction) {
        processStep(binder.getBean().id(), stepFunction);
    }

    public void processStep(StepID stepId, Function<StepID, RoutineResponse> stepFunction) {
        RoutineResponse response = stepFunction.apply(stepId);
        if (response.success()) {
            this.updateRoutine(routine);
        }
    }

    private void processRoutine(Function<RoutineID, RoutineResponse> stepFunction) {
        log.debug("Processing routine: " + binder.getBean().routineId());
        RoutineResponse response = stepFunction.apply(binder.getBean().routineId());
        if (response.success()) {
            this.updateRoutine(routine);
        }
    }

    private boolean isActive() {
        return binder.getBean().status().equals(TimeableStatus.ACTIVE);
    }

    private void updateRoutine(Routine routine) {
        this.routine = routine;
        this.updateComponents();
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
            recalculate.setBoolean(routine.autoSync());

            this.setNestedTaskInfoBars();
            taskInfoBarLayout.removeAll();
            taskInfoBarLayout.add(nestedTaskInfoBars);
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
            completed = new Span(routine.status().toString() + " '" + routine.name() + "!");
        } else {
            completed = new Span(routine.status().toString() + " '" + routine.name() + "' in " +
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

    private void setNestedTaskInfoBars() {
        if (nestedTaskInfoBars == null) {
            nestedTaskInfoBars = new ArrayList<>();
        } else {
            nestedTaskInfoBars.clear();
        }

        RoutineStep current = binder.getBean();
        while (current.parentId() != null) {
            current = routine.steps().get(current.parentId());
            createTaskInfoBar(current);
        }
    }

    private void createTaskInfoBar(Timeable timeable) {
        TaskInfoBar taskInfoBar = new TaskInfoBar(timeable, this);
        if (!timeable.description().isBlank()) taskInfoBar.setOpened(true);
        nestedTaskInfoBars.add(0, taskInfoBar);
    }

    private void initComponents() {
        this.setNestedTaskInfoBars();

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

        recalculate = new RoutineCardBooleanButton(VaadinIcon.REFRESH.create());
        recalculate.setBoolean(routine.autoSync());
        recalculate.addClickListener(event -> this.processRoutine(routineId -> {
            boolean autoSync = routine.autoSync();
            recalculate.setEnabled(!autoSync);
            return controller.setAutoSync(routineId, !autoSync);
        }));

        skip = new RoutineCardButton(VaadinIcon.STEP_FORWARD.create());
        skip.addClickListener(event -> this.processStep(
                controller::skipRoutineStep));

        postpone = new RoutineCardButton(VaadinIcon.TIME_FORWARD.create());
        postpone.addClickListener(event -> this.processStep(
                controller::postponeRoutineStep));

        close = new RoutineCardButton(VaadinIcon.CLOSE.create());
        close.addClickListener(event -> this.processStep(
        stepId -> controller.setRoutineStepExcluded(stepId, true)));

        toTaskTree = new RoutineCardButton(VaadinIcon.SEARCH.create());
        toTaskTree.addClickListener(event -> {
            UI.getCurrent().navigate(TreeView.class);
            TreeView treeView = (TreeView) UI.getCurrent().getCurrentView();
            TaskNetworkGraph taskNetworkGraph = treeView.networkGraph();

            TaskNode currentNode = binder.getBean().node();
            log.debug("Navigating to task tree to node: " + currentNode);
            TaskEntry newRootEntry = taskNetworkGraph.taskEntryDataProvider().linkTaskEntriesMap().getFirst(currentNode.id());
            if (newRootEntry == null) newRootEntry = new TaskEntry(null, binder.getBean().node());
            treeView.firstTaskTreeGrid().nestedTabs().onSelectNewRootEntry(newRootEntry);
        });

        currentTaskName = new Span(binder.getBean().task().name());
        ReadOnlyHasValue<String> taskName = new ReadOnlyHasValue<>(
                text -> currentTaskName.setText(text));
        binder.forField(taskName)
                .bindReadOnly(step -> step.task().name());

        timer = new CountdownTimer(binder.getBean());

        taskInfoBarLayout = new VerticalLayout();
        this.updateComponents();

        description = new TextArea();
        binder.forField(description)
                .bind(step -> step.task().description(),
                        (step, text) -> step.task().description(text));
        description.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        description.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                Change change = Change.update(binder.getBean().task());
                controller.requestChangeAsync(change);
            }
        });
    }

    private void layout() {
        this.setSpacing(false);
        this.addClassName("card");

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
        auxActions.add(close, toTaskTree, recalculate, play, pause, skip, postpone);

        VerticalLayout middle = new VerticalLayout();
        middle.addClassName("middle");
        middle.setSpacing(false);
        middle.setPadding(false);
        middle.add(description, auxActions);

        description.setWidthFull();

        lower.add(left, middle, right);

        taskInfoBarLayout.setPadding(false);
        taskInfoBarLayout.setSpacing(false);
        taskInfoBarLayout.setMargin(false);

        this.add(taskInfoBarLayout);
        this.add(header, lower);
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

    @Getter
    @Setter
    private static class RoutineCardBooleanButton extends Div {
        private boolean enabled = true;

        public RoutineCardBooleanButton(Icon icon) {
            super(icon);

            icon.addClassNames(
                    LumoUtility.IconSize.MEDIUM);
        }

        public void setBoolean(boolean enabled) {
            if (enabled) {
                this.addClassName(K.COLOR_PRIMARY);
                this.removeClassName(K.COLOR_UNSELECTED);
            } else {
                this.removeClassName(K.COLOR_PRIMARY);
                this.addClassName(K.COLOR_UNSELECTED);
            }
        }
    }

    private static class RoutineCardToggleableButton extends RoutineCardBooleanButton {
        public RoutineCardToggleableButton(Icon icon) {
            super(icon);
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.setBoolean(enabled);
            super.setEnabled(enabled);
        }
    }
}