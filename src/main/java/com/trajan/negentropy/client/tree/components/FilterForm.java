package com.trajan.negentropy.client.tree.components;

import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.stream.Collectors;

@Accessors(fluent = true)
@Getter
public class FilterForm extends FormLayout {
    private final ClientDataController controller;

    private TextField name;
    private Checkbox innerJoinIncludedTags;
    private TagComboBox tagsToExclude;
    private TagComboBox tagsToInclude;
    private Button resetButton;

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

        innerJoinIncludedTags = new Checkbox("All of the included tags");
        innerJoinIncludedTags.addValueChangeListener(event -> {
            TaskFilter filter = controller.dataProvider().getFilter();
            filter.innerJoinIncludedTags(innerJoinIncludedTags.getValue());
            controller.dataProvider().refreshAll();
        });

        tagsToExclude = new TagComboBox("Filter: Excluded Tags", controller);
        tagsToExclude.setClearButtonVisible(true);
        tagsToExclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        tagsToExclude.addValueChangeListener(event -> {
            TaskFilter filter = controller.dataProvider().getFilter();
            filter.excludedTagIds(tagsToExclude.getValue().stream()
                    .map(Tag::id)
                    .collect(Collectors.toSet()));
            controller.dataProvider().refreshAll();
        });

        tagsToInclude = new TagComboBox("Filter: Include Tags", controller);
        tagsToInclude.setClearButtonVisible(true);
        tagsToInclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        tagsToInclude.addValueChangeListener(event -> {
            TaskFilter filter = controller.dataProvider().getFilter();
            filter.includedTagIds(tagsToInclude.getValue().stream()
                    .map(Tag::id)
                    .collect(Collectors.toSet()));
            controller.dataProvider().refreshAll();
        });

        resetButton = new Button("Reset");

        this.add(name, innerJoinIncludedTags, resetButton, tagsToInclude, tagsToExclude);
    }

    private void configureInteractions() {
        resetButton.addClickListener(e -> {
            name.clear();
            innerJoinIncludedTags.clear();
            tagsToExclude.clear();
            tagsToInclude.clear();
        });
    }

    private void configureLayout() {
        this.setColspan(name, 2);
        this.setColspan(tagsToInclude, 2);
        this.setColspan(tagsToExclude, 2);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 2),
                new ResponsiveStep("600px", 4),
                new ResponsiveStep("1200px", 6));

        this.setWidthFull();
    }
}