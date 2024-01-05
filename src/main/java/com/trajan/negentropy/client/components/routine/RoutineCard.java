package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.TreeView;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.TriConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RoutineCard extends VerticalLayout {
    private final UILogger log = new UILogger();

    @Getter private final UIController controller;

    private List<Component> nestedTaskInfoBars;
    private RoutineCardButton next;
    private RoutineCardToggleableButton prev;
    private RoutineCardButton pause;
    private RoutineCardButton play;
    private RoutineCardButton skip;
    // TODO: Temporarily removed until strategy for disabled auto-calc determined
//    private RoutineCardBooleanButton recalculate;
    private RoutineCardButton postpone;
    private RoutineCardButton close;
    private RoutineCardButton kickUp;
    private RoutineCardButton pushForward;
    private RoutineCardButton toTaskTree;
    private Span currentTaskName;
    private CountdownTimer timer;
    private TextArea description;

    private VerticalLayout taskInfoBarLayout;

    @Getter private Routine routine;
    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    private final RoutineStepTreeGrid routineStepTreeGrid;

    public RoutineCard(Routine routine, UIController controller, RoutineStepTreeGrid routineStepTreeGrid) {
        this.controller = controller;
        this.routineStepTreeGrid = routineStepTreeGrid;

        this.routine = routine;
        log.debug("Routine card created for routine <" + routine.name() + "> with " + routine.countSteps() + " steps.");

        if (routine.status().equals(TimeableStatus.COMPLETED)) {
            this.setCardCompleted();
        } else {
            binder.setBean(routine.currentStep());
            initComponents();
        }

        layout();

        Registration registration = controller.registerRoutineCard(routine.id(), r -> {
            log.debug("Updated routine: " + r);
            updateRoutine(r);
        });

        this.addDetachListener(event -> registration.remove());
    }

    public void processStep(TriConsumer<StepID, Consumer<RoutineResponse>, Consumer<RoutineResponse>> stepFunction) {
        processStep(binder.getBean().id(), stepFunction);
    }

    public void processStep(StepID stepId, TriConsumer<StepID, Consumer<RoutineResponse>, Consumer<RoutineResponse>> stepFunction) {
        stepFunction.accept(stepId,
                response -> this.updateRoutine(routine),
                null);
    }

    private void processRoutine(TriConsumer<RoutineID, Consumer<RoutineResponse>, Consumer<RoutineResponse>> stepFunction) {
        stepFunction.accept(binder.getBean().routineId(),
                response -> this.updateRoutine(routine),
                null);
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
            kickUp.setEnabled(binder.getBean().parentId() != null);
            pushForward.setEnabled(binder.getBean().parentId() != null);
            toTaskTree.setEnabled(binder.getBean().node() != null);

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

        Span finalStatus;
        if (routine.startTime() == null) {
            finalStatus = new Span(routine.status().toString() + " '" + routine.name() + "'");
        } else {
            if (routine.startTime() != null && routine.finishTime() != null) {
                log.debug("Routine start time: " + routine.startTime());
                log.debug("Routine finish time: " + routine.finishTime());
                finalStatus = new Span(routine.status().toString() + " '" + routine.name() + "' in " +
                        DurationConverter.toPresentation(routine.children().stream()
                                .reduce(Duration.ZERO,
                                        (duration, step) -> duration.plus(TimeableUtil.get()
                                                .getElapsedActiveDuration(step, step.finishTime())),
                                        Duration::plus))
                        + "!");
            } else {
                finalStatus = new Span(routine.status().toString() + " '" + routine.name() + "'");
            }
        }

        finalStatus.addClassName("name");
        this.add(finalStatus);
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
            TaskInfoBar taskInfoBar = new TaskInfoBar(current, this, true);
            if (!current.description().isBlank()) taskInfoBar.setOpened(true);
            nestedTaskInfoBars.add(0, taskInfoBar);
        }

        if (nestedTaskInfoBars.isEmpty()) {
            nestedTaskInfoBars.add(new TaskInfoBar(current, this, false));
        }
    }

    private void confirmRoutineCompletion() {
        if (controller.services().routine().completeStepWouldFinishRoutine(binder.getBean().id())
                && (routine.hasExcludedSteps() || controller.services().routine().hasFilteredOutSteps(routine.id()))) {
            Dialog confirmComplete = new Dialog();
            confirmComplete.setHeaderTitle("Complete routine? Routine still has excluded tasks or tasks filtered steps.");

            LinkedList<Button> buttonList = new LinkedList<>();
            Button complete = new Button("Complete");
            complete.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            complete.addClickListener(e -> {
                this.processStep(controller::completeRoutineStep);
                confirmComplete.close();
            });
            buttonList.add(complete);

            Button exclude = new Button("Exclude");
            exclude.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            exclude.addClickListener(e -> {
                this.processStep(controller::excludeRoutineStep);
                confirmComplete.close();
            });
            buttonList.add(exclude);

            Button suspend = new Button("Suspend");
            suspend.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            suspend.addClickListener(e -> {
                this.processStep(controller::pauseRoutineStep);
                confirmComplete.close();
            });
            buttonList.add(suspend);

            Button cancel = new Button("Cancel");
            cancel.addClickListener(e -> confirmComplete.close());

            HorizontalLayout buttons = new HorizontalLayout();
            buttons.setJustifyContentMode(JustifyContentMode.START);
            buttons.setWidthFull();
            buttonList.forEach(buttons::add);

            HorizontalLayout footer = new HorizontalLayout(buttons, cancel);
            footer.setJustifyContentMode(JustifyContentMode.END);
            footer.setWidthFull();

            confirmComplete.getFooter().add(footer);
            confirmComplete.open();
        } else {
            this.processStep(controller::completeRoutineStep);
        }
    }

    private void initComponents() {
        this.setNestedTaskInfoBars();

        next = new RoutineCardButton(VaadinIcon.CHEVRON_RIGHT.create());
        next.addClickListener(event -> {
            if (binder.getBean().task().cleanup()) {
                Dialog cleanupDialog = new Dialog();
                cleanupDialog.setHeaderTitle("Has everything needed been cleaned up?");

                Button yes = new Button("Yes");
                yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                yes.addClickListener(e -> {
                    cleanupDialog.close();
                    this.confirmRoutineCompletion();
                });

                Button no = new Button("No");
                no.addClickListener(e -> cleanupDialog.close());

                HorizontalLayout buttons = new HorizontalLayout();
                buttons.setJustifyContentMode(JustifyContentMode.BETWEEN);
                buttons.setWidthFull();
                buttons.add(yes, no);

                cleanupDialog.getFooter().add(buttons);
                cleanupDialog.open();
            } else {
                this.confirmRoutineCompletion();
            }
        });

        prev = new RoutineCardToggleableButton(VaadinIcon.CHEVRON_LEFT.create());
        prev.addClickListener(event -> this.processStep(
                controller::previousRoutineStep));

        play = new RoutineCardButton(VaadinIcon.PLAY.create());
        play.addClickListener(event -> this.processStep(
                controller::startRoutineStep));

        pause = new RoutineCardButton(VaadinIcon.PAUSE.create());
        pause.addClickListener(event -> this.processStep(
                controller::pauseRoutineStep));

//        recalculate = new RoutineCardBooleanButton(VaadinIcon.REFRESH.create());
//        recalculate.setBoolean(routine.autoSync());
//        recalculate.addClickListener(event -> {
//            boolean autoSync = routine.autoSync();
//            controller.setAutoSync(routine.id(), !autoSync,
//                    r -> recalculate.setEnabled(!autoSync),
//                    null);
//        });

        skip = new RoutineCardButton(VaadinIcon.STEP_FORWARD.create());
        skip.addClickListener(event -> this.processStep(
                controller::skipRoutineStep));

        postpone = new RoutineCardButton(VaadinIcon.TIME_FORWARD.create());
        postpone.addClickListener(event -> this.processStep(
                controller::postponeRoutineStep));

        close = new RoutineCardButton(VaadinIcon.CLOSE.create());
        close.addClickListener(event -> this.processStep(
                controller::excludeRoutineStep));

        kickUp = new RoutineCardButton(VaadinIcon.LEVEL_UP.create());
        kickUp.setEnabled(binder.getBean().parentId() != null);
        kickUp.addClickListener(event -> this.processStep(
                controller::kickUpStep));

        pushForward = new RoutineCardButton(VaadinIcon.LEVEL_RIGHT.create());
        pushForward.setEnabled(binder.getBean().parentId() != null);
        pushForward.addClickListener(event -> this.processStep(
                controller::pushStepForward));

        toTaskTree = new RoutineCardButton(VaadinIcon.TREE_TABLE.create());
        toTaskTree.setEnabled(binder.getBean().node() != null);
        toTaskTree.addClickListener(event -> RoutineCard.toTaskTree(
                () -> binder.getBean().node(), controller));

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
        timer.addClassName("name-timer");

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setSpacing(false);
        header.getThemeList().add("spacing-s");
        header.setWidthFull();
        header.add(currentTaskName, timer);

        HorizontalLayout auxActions = new HorizontalLayout();
        auxActions.addClassName("footer");
        auxActions.setWidthFull();
//        auxActions.add(close, toTaskTree, recalculate, play, pause, skip, postpone);
        auxActions.add(close, postpone, toTaskTree, play, pause, kickUp, pushForward, skip);

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

    public static void toTaskTree(Supplier<TaskNode> nodeSupplier, UIController controller) {
        UI ui = UI.getCurrent();
        ui.access(() -> {
            ui.navigate(TreeView.class);
            TreeView treeView = (TreeView) ui.getCurrentView();

            TaskNode currentNode = nodeSupplier.get();
            TaskEntry newRootEntry = controller.taskEntryDataProvider().linkTaskEntriesMap().getFirst(currentNode.id());
            if (newRootEntry == null) newRootEntry = new TaskEntry(null, currentNode);
            treeView.firstTaskTreeGrid().nestedTabs().selectNewRootEntry(newRootEntry);
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