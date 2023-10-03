package com.trajan.negentropy.client.components.searchandfilterform;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TaskFilterForm extends AbstractSearchAndFilterForm {
    private final UIController controller;

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
        this.add(topLayout, filterOptions, tagsToInclude, tagsToExclude);
    }
}
