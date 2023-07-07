package com.trajan.negentropy.client.components;

import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@Getter
public class FilterForm extends FormLayout {
    private final ClientDataController controller;

    private TextField name;
    private Button resetButton;
    private HorizontalLayout topLayout;
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
        name.setPlaceholder("Search By Name");
        name.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        name.setValueChangeMode(ValueChangeMode.EAGER);
        name.addValueChangeListener(event -> {
            TaskFilter filter = controller.dataProvider().getFilter();
            filter.name(name.getValue());
            controller.dataProvider().refreshAll();
        });

        resetButton = new Button("Reset");
        resetButton.setMaxWidth("5em");

        filterOptions = new CheckboxGroup<>();
        filterOptions.setWidthFull();
        filterOptions.setItems(TaskFilter.OPTION_TYPES());
        filterOptions.addValueChangeListener(event -> {
            TaskFilter filter = controller.dataProvider().getFilter();
            filter.options(filterOptions.getValue());
            controller.dataProvider().refreshAll();
        });
        filterOptions.setValue(controller.settings().filter().options());

        topLayout = new HorizontalLayout(name, resetButton);
        name.setWidthFull();
        topLayout.setWidthFull();
        topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        tagsToExclude = new TagComboBox("Filter: Excluded Tags", controller);
        tagsToExclude.setClearButtonVisible(true);
        tagsToExclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        tagsToExclude.addValueChangeListener(event -> {
            controller.settings().filter().excludedTagIds(tagsToExclude.getValue().stream()
                    .map(Tag::id)
                    .collect(Collectors.toSet()));
            controller.dataProvider().refreshAll();
        });
        // TODO: Load tags from settings

        tagsToInclude = new TagComboBox("Filter: Include Tags", controller);
        tagsToInclude.setClearButtonVisible(true);
        tagsToInclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        tagsToInclude.addValueChangeListener(event -> {
            controller.settings().filter().includedTagIds(tagsToInclude.getValue().stream()
                    .map(Tag::id)
                    .collect(Collectors.toSet()));
            controller.dataProvider().refreshAll();
        });
        // TODO: Load tags from settings

        this.add(topLayout, filterOptions, tagsToInclude, tagsToExclude);
    }

    private void configureInteractions() {
        resetButton.addClickListener(e -> {
            name.clear();
            filterOptions.setValue(Set.of());
            tagsToExclude.clear();
            tagsToInclude.clear();
        });
    }

    private void configureLayout() {
//        this.setColspan(topLayout, 4);
//        this.setColspan(tagsToInclude, 4);
//        this.setColspan(tagsToExclude, 4);
//
//        this.setResponsiveSteps(
//                new ResponsiveStep("0", 2),
//                new ResponsiveStep("1200px", 4));

        this.setWidthFull();
    }
}