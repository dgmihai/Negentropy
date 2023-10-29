package com.trajan.negentropy.client.components.searchandfilterform;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;

@Getter
public class TaskFilterForm extends AbstractSearchAndFilterForm {
    private final UIController controller;
    private Button goToCreateNewTaskFormButton;
    private HorizontalLayout nameFieldLayout;

    protected Binder<TaskTreeFilter> binder = new BeanValidationBinder<>(TaskTreeFilter.class);

    public TaskFilterForm(UIController controller) {
        this.controller = controller;

        configureFields();
        configureInteractions();
        configureLayout();
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        goToCreateNewTaskFormButton = new Button(VaadinIcon.FILE_ADD.create());
        goToCreateNewTaskFormButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        nameFieldLayout = new HorizontalLayout(name, goToCreateNewTaskFormButton);
    }

    @Override
    protected void configureLayout() {
        topLayout = new HorizontalLayout(name, goToCreateNewTaskFormButton, resetButton);
        topLayout.setWidthFull();
        topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        this.add(topLayout, filterOptions, tagsToInclude, tagsToExclude);
    }
}
