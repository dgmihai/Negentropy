package com.trajan.negentropy.client.components.filterform;

import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.ReadOnlySettableFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@Getter
public class FilterForm extends ReadOnlySettableFormLayout {
    protected final ClientDataController controller;

    protected Binder<TaskFilter> binder = new BeanValidationBinder<>(TaskFilter.class);

    private TextField name;
    private Button resetButton;
    private HorizontalLayout topLayout;
    @PropertyId("options")
    private CheckboxGroup<String> filterOptions;
    private TagComboBox tagsToExclude;
    private TagComboBox tagsToInclude;

    public FilterForm(ClientDataController controller) {
        this.controller = controller;

        this.addClassName("filter-layout");

        configureFields();
        configureInteractions();
        configureLayout();
    }

    private void configureFields() {
        name = new TextField();
        name.setClearButtonVisible(true);
        name.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        name.setWidthFull();
        name.setValueChangeMode(ValueChangeMode.LAZY);
        name.setValueChangeTimeout(100);
        binder.bind(name, TaskFilter::name, TaskFilter::name);

        resetButton = new Button("Reset");
        resetButton.setMaxWidth("5em");

        filterOptions = new CheckboxGroup<>();
        filterOptions.setWidthFull();
        filterOptions.setItems(TaskFilter.OPTION_TYPES());
        binder.bind(filterOptions, TaskFilter::options, TaskFilter::options);

        topLayout = new HorizontalLayout(name, resetButton);
        topLayout.setWidthFull();
        topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        tagsToExclude = new TagComboBox("Filter: Excluded Tags", controller);
        tagsToExclude.setClearButtonVisible(true);
        tagsToExclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        binder.bind(tagsToExclude,
                filter -> filter.excludedTagIds().stream()
                        .map(tagId -> controller.services().query().fetchTag(tagId))
                        .collect(Collectors.toSet()),
                (filter, excludedTags) -> filter.excludedTagIds(excludedTags.stream()
                        .map(Tag::id)
                        .collect(Collectors.toSet())));

        tagsToInclude = new TagComboBox("Filter: Include Tags", controller);
        tagsToInclude.setClearButtonVisible(true);
        tagsToInclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        binder.bind(tagsToInclude,
                filter -> filter.includedTagIds().stream()
                        .map(tagId -> controller.services().query().fetchTag(tagId))
                        .collect(Collectors.toSet()),
                (filter, includedTags) -> filter.includedTagIds(includedTags.stream()
                        .map(Tag::id)
                        .collect(Collectors.toSet())));

        this.add(topLayout, filterOptions, tagsToInclude, tagsToExclude);
    }

    protected void configureInteractions() {
        resetButton.addClickListener(e -> {
            name.clear();
            filterOptions.setValue(Set.of());
            tagsToExclude.clear();
            tagsToInclude.clear();
        });
    }

    private void configureLayout() {
        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("1600px", 2));

        this.setWidthFull();
    }
}