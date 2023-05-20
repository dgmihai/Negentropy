package com.trajan.negentropy.client.routine.components;

import com.trajan.negentropy.client.tree.components.InlineIconButton;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TaskInfoBar extends Details {
    private final H3 name;
    private final InlineIconButton skip;
    private final TextArea description;

    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    public TaskInfoBar() {
        name = new H3();

        skip = new InlineIconButton(VaadinIcon.CLOSE.create());

        description = new TextArea();
        binder.forField(description)
                .bindReadOnly(s -> s.task().description());

        HorizontalLayout summary = new HorizontalLayout(name, skip);

        summary.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        this.setSummary(summary);
        summary.setWidthFull();
        this.setContent(description);
    }

    public void setStep(RoutineStep step) {
        binder.setBean(step);

        name.setText(binder.getBean().task().name());

        description.setWidthFull();

        this.setWidthFull();
    }
}