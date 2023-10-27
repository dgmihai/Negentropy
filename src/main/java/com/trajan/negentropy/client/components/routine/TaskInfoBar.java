package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import lombok.Getter;

@Getter
public class TaskInfoBar extends Details {
    private final RoutineCard parent;
    private final UIController controller;

    private final Span name;
    private final ETATimer timer;
    private final Span description;

    private final Binder<Timeable> binder = new BeanValidationBinder<>(Timeable.class);

    public TaskInfoBar(Timeable timeable, RoutineCard parent) {
        super();
        this.parent = parent;
        controller = parent.controller();

        this.addClassName("bar");

        binder.setBean(timeable);

        name = new Span();
        name.addClassName("date");
        name.setWidthFull();

        timer = new ETATimer(binder.getBean());
        timer.addClassNames("date", "timer");
//        timer.run(timer.timeable().status().equals(TimeableStatus.ACTIVE));

        description = new Span();
        description.addClassName("description");
        description.setWidthFull();

        HorizontalLayout upper = new HorizontalLayout(name, timer);
        upper.addClassName("header");
        upper.getThemeList().add("spacing-s");
        upper.setPadding(false);
        upper.setWidthFull();
        upper.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout lower = new VerticalLayout(description);
        lower.setWidthFull();
        lower.setSpacing(false);
        lower.addClassName(LumoUtility.Padding.Vertical.NONE);

        if (binder.getBean() instanceof RoutineStep step) {
            TaskInfoBarButton next = new TaskInfoBarButton(VaadinIcon.CHEVRON_RIGHT.create());
            next.addClickListener(event -> parent.processStep(step.id(),
                    controller::completeRoutineStep));

            TaskInfoBarButton prev = new TaskInfoBarButton(VaadinIcon.CHEVRON_LEFT.create());
            prev.addClickListener(event -> parent.processStep(step.id(),
                    controller::previousRoutineStep));

            TaskInfoBarButton skip = new TaskInfoBarButton(VaadinIcon.STEP_FORWARD.create());
            skip.addClickListener(event -> parent.processStep(step.id(),
                    controller::skipRoutineStep));

            TaskInfoBarButton postpone = new TaskInfoBarButton(VaadinIcon.TIME_FORWARD.create());
            postpone.addClickListener(event -> parent.processStep(step.id(),
                    controller::postponeRoutineStep));

            TaskInfoBarButton close = new TaskInfoBarButton(VaadinIcon.CLOSE.create());
            close.addClickListener(event -> parent.processStep(step.id(),
                    stepID -> controller.setRoutineStepExcluded(stepID, true)));

            HorizontalLayout buttons = new HorizontalLayout(prev, close, skip, postpone, next);
            buttons.setWidthFull();
            buttons.setJustifyContentMode(JustifyContentMode.BETWEEN);
            lower.add(buttons);
        }

        this.setSummary(upper);
        this.setContent(lower);

        this.setWidthFull();

        this.setTimeable(timeable);
        this.addThemeVariants(DetailsVariant.SMALL);
        this.addClassName(Margin.NONE);
    }

    public void setTimeable(Timeable timeable) {
        binder.setBean(timeable);

        name.setText(binder.getBean().name());

        timer.setTimeable(timeable);

        description.setText(binder.getBean().description());
        description.setWidthFull();

        this.setWidthFull();
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