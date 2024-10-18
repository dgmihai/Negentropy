package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;

@Getter
public class TaskInfoBar extends VerticalLayout {
    private final RoutineCard parent;
    private final UIController controller;

    private final Span name;
    private final ETATimer timer;
    private Span description;
    private InlineIconButton descriptionButton;
    private boolean expanded;

    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    public TaskInfoBar(RoutineStep step, RoutineCard parent, boolean withContent) {
        super();
        this.parent = parent;
        this.expanded = withContent;
        controller = parent.controller();

        this.addClassName("bar");

        binder.setBean(step);

        descriptionButton = new InlineIconButton(VaadinIcon.COMMENT_ELLIPSIS_O.create());
        descriptionButton.setTooltipText("Show Notes");

        InlineIconButton inlineTaskViewButton = new InlineIconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
        ContextMenu menu = new ContextMenu(inlineTaskViewButton);
        menu.setOpenOnClick(true);

        menu.addItem("Next").addClickListener(event ->
                parent.processStep(step.id(), controller::completeRoutineStep));

        menu.addItem("Previous").addClickListener(event -> parent.processStep(
                step.id(),
                controller::previousRoutineStep));

        menu.addItem("Skip").addClickListener(event ->
                parent.processStep(step.id(),
                        controller::skipRoutineStep));

        menu.addItem("Postpone").addClickListener(event ->
                parent.processStep(step.id(),
                        controller::postponeRoutineStep));

        menu.addItem("Exclude").addClickListener(event ->
                parent.processStep(step.id(),
                        (stepID, s, f) -> controller.setRoutineStepExcluded(stepID, true, s, f)));

        menu.addItem("View in Task Tree").addClickListener(event ->
                RoutineCard.toTaskTree(step::node, controller));

        menu.addItem("Jump to Step").addClickListener(event ->
                parent.processStep(step.id(),
                        controller::jumpToRoutineStep));

        name = new Span();
        name.addClassName("date");
        name.setWidthFull();

        description = new Span();
        description.addClassName("description");
        description.setWidthFull();

        timer = new ETATimer(binder.getBean(), parent.routine());
        timer.addClassNames("date", "timer");

        HorizontalLayout summary = new HorizontalLayout(
                new Span(descriptionButton, inlineTaskViewButton, name),
                timer);
        summary.addClassName("header");
        summary.getThemeList().add("spacing-s");
        summary.setPadding(false);
        summary.setWidthFull();
        summary.setJustifyContentMode(JustifyContentMode.BETWEEN);

        this.add(summary);

        this.setWidthFull();
        this.setStep(step);
        this.setPadding(false);
        this.setMargin(false);
        this.setSpacing(false);
    }

    public void setStep(RoutineStep step) {
        binder.setBean(step);

        name.setText(binder.getBean().name());

        timer.setTimeable(step, parent.routine());

        String descriptionText = step.description();
        description.setText(descriptionText);

        descriptionButton.addClassName(descriptionText.isBlank()
                ? K.COLOR_TRANSPARENT : K.COLOR_PRIMARY);

        descriptionButton.addClickListener(e -> {
            if (!binder.getBean().description().isBlank()) {
                description.setVisible(!description.isVisible());
            }
        });

        this.add(description);
    }

    private static class TaskInfoBarButton extends Div {
        public TaskInfoBarButton(Icon icon) {
            super(icon);

            icon.addClassNames(
                    LumoUtility.IconSize.SMALL,
                    K.COLOR_PRIMARY);
        }
    }
}