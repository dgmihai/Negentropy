package com.trajan.negentropy.client.components.searchandfilterform;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter.NestableTaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;

@Getter
public class TaskNodeFilterForm extends AbstractSearchAndFilterForm {
    private final UIController controller;

    protected Binder<NestableTaskNodeTreeFilter> binder = new BeanValidationBinder<>(NestableTaskNodeTreeFilter.class);

    private Checkbox completed;
    private Checkbox recurring;
    private Checkbox ignoreScheduling;
    private Checkbox onlyStarred;
    private Checkbox nested;

    public TaskNodeFilterForm(UIController controller) {
        this.controller = controller;

        this.addClassName("filter-layout");

        configureFields();
        configureInteractions();
        configureLayout();
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        completed = new Checkbox("Hide Completed");
        binder.bind(completed,
                filter -> filter.completed() != null,
                (filter, value) -> {
                        if (value) {
                            filter.completed(false);
                        } else {
                            filter.completed(null);
                        }}
        );

        recurring = new Checkbox("Hide Recurring");
        binder.bind(recurring,
                filter -> filter.recurring() != null,
                (filter, value) -> {
                    if (value) {
                        filter.recurring(false);
                    } else {
                        filter.recurring(null);
                    }}
        );

        nested = new Checkbox("Nested");
        binder.bind(nested,
                NestableTaskNodeTreeFilter::nested,
                NestableTaskNodeTreeFilter::nested);

        ignoreScheduling = new Checkbox("Ignore Scheduling");
        binder.bind(ignoreScheduling,
                filter -> filter.ignoreScheduling() != null ? filter.ignoreScheduling() : false,
                TaskTreeFilter::ignoreScheduling);

        onlyStarred = new Checkbox("Only Starred");
        binder.bind(onlyStarred,
                filter -> filter.onlyStarred() != null ? filter.onlyStarred() : false,
                TaskTreeFilter::onlyStarred);

//        HorizontalLayout additionalCheckboxes = new HorizontalLayout(completed, recurring, hasChildren, ignoreScheduling);
//        additionalCheckboxes.setWidthFull();
//        additionalCheckboxes.setJustifyContentMode(JustifyContentMode.START);
//        additionalCheckboxes.setPadding(false);
//        additionalCheckboxes.setMargin(false);

        VerticalLayout checkboxLayout = new VerticalLayout(
                filterOptions, new Span(completed, nested, recurring, hasChildren,
                ignoreScheduling, onlyStarred));
        checkboxLayout.setWidthFull();
        checkboxLayout.setSpacing(false);
        checkboxLayout.setPadding(false);
        checkboxLayout.setMargin(false);

        this.add(topLayout, checkboxLayout, tagsToInclude, tagsToExclude);
    }
}