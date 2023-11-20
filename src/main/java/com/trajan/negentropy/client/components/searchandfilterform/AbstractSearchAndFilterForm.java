package com.trajan.negentropy.client.components.searchandfilterform;

import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.ReadOnlySettableFormLayout;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public abstract class AbstractSearchAndFilterForm extends ReadOnlySettableFormLayout {
    protected TextField name;
    protected Button resetButton;
    protected HorizontalLayout topLayout;
    @PropertyId("options")
    protected CheckboxGroup<String> filterOptions;
    protected TagComboBox tagsToExclude;
    protected TagComboBox tagsToInclude;
    protected Checkbox hasChildren;

    public abstract Binder<? extends TaskTreeFilter> binder();
    protected abstract UIController controller();

    protected void configureInteractions() {
        resetButton.addClickListener(e -> {
            name.clear();
            filterOptions.setValue(Set.of());
            tagsToExclude.clear();
            tagsToInclude.clear();
        });
    }

    protected void configureLayout() {
        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("1600px", 2));

        this.setWidthFull();
    }

    protected void configureFields() {
        this.addClassName("filter-layout");

        name = new TextField();
        name.setClearButtonVisible(true);
        name.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        name.setWidthFull();
        name.setValueChangeMode(ValueChangeMode.LAZY);
        name.setValueChangeTimeout(100);
        binder().bind(name, TaskTreeFilter::name, TaskTreeFilter::name);

        resetButton = new Button("Reset");
        resetButton.setMaxWidth("5em");

        filterOptions = new CheckboxGroup<>();
        filterOptions.setWidthFull();
        filterOptions.setItems(TaskTreeFilter.OPTION_TYPES());
        binder().bind(filterOptions, TaskTreeFilter::options, TaskTreeFilter::options);

        topLayout = new HorizontalLayout(name, resetButton);
        topLayout.setWidthFull();
        topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        tagsToExclude = new TagComboBox("Filter: Exclude Tags", controller());
        tagsToExclude.setClearButtonVisible(true);
        tagsToExclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        binder().bind(tagsToExclude,
                filter -> filter.excludedTagIds().stream()
                        .map(tagId -> controller().services().query().fetchTag(tagId))
                        .collect(Collectors.toSet()),
                (filter, excludedTags) -> filter.excludedTagIds(excludedTags.stream()
                        .map(Tag::id)
                        .collect(Collectors.toSet())));

        tagsToInclude = new TagComboBox("Filter: Include Tags", controller());
        tagsToInclude.setClearButtonVisible(true);
        tagsToInclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        binder().bind(tagsToInclude,
                filter -> filter.includedTagIds().stream()
                        .map(tagId -> controller().services().query().fetchTag(tagId))
                        .collect(Collectors.toSet()),
                (filter, includedTags) -> filter.includedTagIds(includedTags.stream()
                        .map(Tag::id)
                        .collect(Collectors.toSet())));

        hasChildren = new Checkbox("Only Parents");
        binder().bind(hasChildren,
                filter -> filter.hasChildren() != null,
                (filter, value) -> {
                    if (value) {
                        filter.hasChildren(false);
                    } else {
                        filter.hasChildren(null);
                    }
        });
    }
}
