package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TaskInfoBar extends VerticalLayout {
    private final UIController controller;

    private final Span name;
    private final RoutineTimer timer;
    private final Span description;

    private final Binder<Timeable> binder = new BeanValidationBinder<>(Timeable.class);

    public TaskInfoBar(Timeable timeable, UIController controller) {
        this.controller = controller;

        this.addClassName("bar");
        this.setSpacing(false);

        binder.setBean(timeable);

        name = new Span();
        name.addClassName("date");
        name.setWidthFull();

        timer = new RoutineTimer(binder.getBean(), controller);
        timer.setShowEta(true);
        timer.addClassNames("date", "timer");

        description = new Span();
        description.addClassName("description");
        description.setWidthFull();

        HorizontalLayout upper = new HorizontalLayout(name, timer);
        upper.addClassName("header");
        upper.getThemeList().add("spacing-s");
        upper.setPadding(false);
        upper.setWidthFull();

        HorizontalLayout lower = new HorizontalLayout(description);
        lower.setWidthFull();

        this.add(upper, lower);

        setTimeable(timeable);

        upper.getElement().addEventListener("click",
                e -> lower.setVisible(!lower.isVisible()));
    }

    public void setTimeable(Timeable timeable) {
        binder.setBean(timeable);

        name.setText(binder.getBean().name());

        timer.setTimeable(timeable);

        description.setText(binder.getBean().description());
        description.setWidthFull();

        this.setWidthFull();
    }
}