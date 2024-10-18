package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.TreeView;
import com.trajan.negentropy.client.components.YesNoDialog;
import com.trajan.negentropy.client.components.fields.DescriptionTextArea;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
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
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ReadOnlyHasValue;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.wontlost.ckeditor.VaadinCKEditor;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.TriConsumer;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RoutineCard extends VerticalLayout {
    private final UILogger log = new UILogger();

    @Getter private final UIController controller;

    private LinkedList<Component> nestedTaskInfoBars;
    private RoutineCardToggleableButton next;
    private RoutineCardToggleableButton prev;
    private RoutineCardToggleableButton pause;
    private RoutineCardToggleableButton play;
    private RoutineCardToggleableButton skip;
    private RoutineCardToggleableButton postpone;
    private RoutineCardToggleableButton close;
    private RoutineCardToggleableButton kickUp;
    private RoutineCardToggleableButton pushForward;
    private RoutineCardButton toTaskTree;
    private Span currentTaskName;
    private CountdownTimer timer;
    private InlineIconButton descriptionSaveButton;
    private InlineIconButton descriptionCancelButton;
    private VaadinCKEditor description;

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

        Registration registration = controller.registerRoutineListener(routine.id(), r -> {
            log.debug("Updated routine: " + r);
            this.routine = r;
            this.updateComponents(true);
        });

        this.addDetachListener(event -> registration.remove());
    }

    public void processStep(TriConsumer<StepID, Consumer<RoutineResponse>, Consumer<RoutineResponse>> stepFunction) {
        processStep(binder.getBean().id(), stepFunction);
    }

    public void processStep(StepID stepId, TriConsumer<StepID, Consumer<RoutineResponse>, Consumer<RoutineResponse>> stepFunction) {
       this.setControlsEnabled(false);
       play.setIcon(VaadinIcon.HOURGLASS.create());
       pause.setIcon(VaadinIcon.HOURGLASS.create());
       stepFunction.accept(stepId, null, null);
    }

    private void processRoutine(TriConsumer<RoutineID, Consumer<RoutineResponse>, Consumer<RoutineResponse>> stepFunction) {
        this.setControlsEnabled(false);
        stepFunction.accept(binder.getBean().routineId(), null, null);
    }

    private boolean isActive() {
        return binder.getBean().status().equals(TimeableStatus.ACTIVE);
    }

    private void updateComponents(boolean updateGrid) {
        this.setControlsEnabled(true);
        if (routine.status().equals(TimeableStatus.COMPLETED)) {
            this.setCardCompleted();
        } else if (routine.status().equalsAny(TimeableStatus.SKIPPED, TimeableStatus.EXCLUDED)) {
            this.setCardSkipped();
        } else {
            binder.setBean(routine.currentStep());
            this.getElement().setAttribute("active", isActive());
            play.setIcon(VaadinIcon.PLAY.create());
            play.setVisible(!isActive());
            pause.setIcon(VaadinIcon.PAUSE.create());
            pause.setVisible(isActive());
            prev.setEnabled(routine.currentPosition() > 0);
            timer.setTimeable(binder.getBean());
            kickUp.setEnabled(binder.getBean().parentId() != null);
            pushForward.setEnabled(binder.getBean().parentId() != null);
            toTaskTree.setEnabled(binder.getBean().node() != null);
            if (descriptionSaveButton != null) descriptionSaveButton.setVisible(false);
            if (descriptionCancelButton != null) descriptionCancelButton.setVisible(false);

            this.setNestedTaskInfoBars();
            taskInfoBarLayout.removeAll();
            taskInfoBarLayout.add(nestedTaskInfoBars);
        }
        if (updateGrid) {
            routineStepTreeGrid.setRoutine(routine);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        play.setEnabled(enabled);
        pause.setEnabled(enabled);
        next.setEnabled(enabled);
        prev.setEnabled(enabled);
        skip.setEnabled(enabled);
        postpone.setEnabled(enabled);
        close.setEnabled(enabled);
        kickUp.setEnabled(enabled);
        pushForward.setEnabled(enabled);
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
                                                .getNestedElapsedActiveDuration(step, step.finishTime())),
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
            nestedTaskInfoBars = new LinkedList<>();
        } else {
            nestedTaskInfoBars.clear();
        }

        Routine focusedRoutine = routineStepTreeGrid.routine();
        if (!controller.settings().hideRoutineTaskBars() && (focusedRoutine == null || focusedRoutine.id().equals(routine.id()))) {
            RoutineStep current = binder.getBean();
            while (current.parentId() != null) {
                current = routine.steps().get(current.parentId());
                TaskInfoBar taskInfoBar = new TaskInfoBar(current, this, true);
                nestedTaskInfoBars.add(0, taskInfoBar);
            }

            if (nestedTaskInfoBars.isEmpty()) {
                nestedTaskInfoBars.add(new TaskInfoBar(current, this, false));
            }
        }
    }

    private void nextSibling() {
        List<StepID> siblings = routine.childAdjacencyMap().get(routine.currentStep().parentId());
        int index = siblings.indexOf(routine.currentStep().id());
        if (index < siblings.size() - 1) {
            routine.currentPosition(routine.currentPosition() + 1);
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

        next = new RoutineCardToggleableButton(VaadinIcon.CHEVRON_RIGHT.create());
        next.addClickListener(e1 -> {
            if (controller.services().routine().stepCanBeCompleted(binder.getBean().id())
                    && binder.getBean().task().cleanup()) {
                YesNoDialog cleanupDialog = new YesNoDialog();
                cleanupDialog.setHeaderTitle("Has everything involved been cleaned up?");
                cleanupDialog.add("Including whatever you were holding/your drink?");

                cleanupDialog.yes().addClickListener(e2 -> {
                    cleanupDialog.close();
                    this.confirmRoutineCompletion();
                });

                cleanupDialog.open();
            } else {
                this.confirmRoutineCompletion();
            }
        });

        prev = new RoutineCardToggleableButton(VaadinIcon.CHEVRON_LEFT.create());
        prev.addClickListener(e -> {
            this.processStep(controller::previousRoutineStep);
        });

        play = new RoutineCardToggleableButton(VaadinIcon.PLAY.create());
        play.addClickListener(e -> {
            this.processStep(controller::startRoutineStep);
        });

        pause = new RoutineCardToggleableButton(VaadinIcon.PAUSE.create());
        pause.addClickListener(e -> {
            this.processStep(controller::pauseRoutineStep);
        });

//        recalculate = new RoutineCardBooleanButton(VaadinIcon.REFRESH.create());
//        recalculate.setBoolean(routine.autoSync());
//        recalculate.addClickListener(event -> {
//            boolean autoSync = routine.autoSync();
//            controller.setAutoSync(routine.id(), !autoSync,
//                    r -> recalculate.setEnabled(!autoSync),
//                    null);
//        });

        skip = new RoutineCardToggleableButton(VaadinIcon.STEP_FORWARD.create());
        skip.addClickListener(e -> {
            this.processStep(controller::skipRoutineStep);
        });

        postpone = new RoutineCardToggleableButton(VaadinIcon.TIME_FORWARD.create());
        postpone.addClickListener(e -> {
            this.processStep(controller::postponeRoutineStep);
        });

        close = new RoutineCardToggleableButton(VaadinIcon.CLOSE.create());
        close.addClickListener(e -> {
            this.processStep(controller::excludeRoutineStep);
        });

        kickUp = new RoutineCardToggleableButton(VaadinIcon.LEVEL_UP.create());
        kickUp.setEnabled(binder.getBean().parentId() != null);
        kickUp.addClickListener(e -> {
            this.processStep(controller::kickUpStep);
        });

        pushForward = new RoutineCardToggleableButton(VaadinIcon.LEVEL_RIGHT.create());
        pushForward.setEnabled(binder.getBean().parentId() != null);
        pushForward.addClickListener(e -> {
            this.processStep(controller::pushStepForward);
        });

        toTaskTree = new RoutineCardButton(VaadinIcon.TREE_TABLE.create());
        toTaskTree.setEnabled(binder.getBean().node() != null);
        toTaskTree.addClickListener(e -> {
            RoutineCard.toTaskTree(() -> binder.getBean().node(), controller);
        });

        currentTaskName = new Span(binder.getBean().task().name());
        ReadOnlyHasValue<String> taskName = new ReadOnlyHasValue<>(
                text -> currentTaskName.setText(text));
        binder.forField(taskName)
                .bindReadOnly(step -> step.task().name());

        timer = new CountdownTimer(binder.getBean());

        taskInfoBarLayout = new VerticalLayout();
        this.updateComponents(false);

        description = DescriptionTextArea.inline(null);
        binder.forField(description)
                .bind(step -> step.task().description(),
                        (step, text) -> step.task().description(text));
        description.addClassName("bordered");
        description.setReadOnly(false);

        descriptionSaveButton = new InlineIconButton(VaadinIcon.CHECK.create());
        descriptionCancelButton = new InlineIconButton(VaadinIcon.CLOSE.create());
        descriptionSaveButton.setVisible(false);
        descriptionCancelButton.setVisible(false);

        description.getElement().addEventListener("click", e -> {
            descriptionSaveButton.setVisible(true);
            descriptionCancelButton.setVisible(true);
        });

        descriptionSaveButton.addClickListener(event -> {
            if (event.isFromClient()) {
                Change change = Change.update(binder.getBean().task());
                controller.requestChangeAsync(change);
                descriptionSaveButton.setVisible(false);
                descriptionCancelButton.setVisible(false);
            }
        });

        descriptionCancelButton.addClickListener(event -> {
            if (event.isFromClient()) {
                descriptionSaveButton.setVisible(false);
                descriptionCancelButton.setVisible(false);
                description.setValue(binder.getBean().task().description());
            }
        });

        Shortcuts.addShortcutListener(description,
                () -> SessionServices.ifNotMobile(descriptionSaveButton::click),
                Key.ENTER);
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
        header.addClassNames("header", "top");
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

        HorizontalLayout descriptionContainer = new HorizontalLayout(description, descriptionSaveButton, descriptionCancelButton);
        descriptionContainer.setWidthFull();
        middle.add(descriptionContainer, auxActions);

        description.setWidthFull();

        lower.add(left, middle, right);

        taskInfoBarLayout.setPadding(false);
        taskInfoBarLayout.setSpacing(false);
        taskInfoBarLayout.setMargin(false);

        this.add(taskInfoBarLayout);
        this.add(header, lower);
        this.setWidthFull();

        header.getElement().addEventListener("click", e -> {
            if (isActive() && pause.enabled()) {
                processStep(controller::pauseRoutineStep);
            } else if (play.enabled()){
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
            treeView.taskTreeGrid().nestedTabs().selectNewRootEntry(newRootEntry);
        });
    }

    private static class RoutineCardButton extends Div {
        public RoutineCardButton(Icon icon) {
            super(icon);
            this.setIcon(icon);
        }

        public void setIcon(Icon icon) {
            this.removeAll();
            this.add(icon);
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

            icon.addClassNames(LumoUtility.IconSize.MEDIUM);
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

        public void setIcon(Icon icon) {
            this.removeAll();
            this.add(icon);
            icon.addClassNames(LumoUtility.IconSize.MEDIUM);
            this.setBoolean(this.enabled);
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