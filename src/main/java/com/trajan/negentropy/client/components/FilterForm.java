package com.trajan.negentropy.client.components;

import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
    private Button resetButton;
    private HorizontalLayout topLayout;
    private Checkbox showOnlyBlocks;
    private Checkbox showCompleted;
    private Checkbox includeParents;
    private Checkbox ignoreScheduling;
    private Checkbox innerJoinIncludedTags;
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

        topLayout = new HorizontalLayout(name, resetButton);
        name.setWidthFull();
        topLayout.setWidthFull();
        topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        showOnlyBlocks = new Checkbox("Show Only Blocks");
        showOnlyBlocks.addValueChangeListener(event -> {
            controller.settings().filter().showOnlyBlocks(showOnlyBlocks.getValue());
            controller.dataProvider().refreshAll();
        });
        showOnlyBlocks.setValue(controller.settings().filter().showOnlyBlocks());

        showCompleted = new Checkbox("Show Completed Tasks");
        showCompleted.addValueChangeListener(event -> {
            controller.settings().filter().showCompleted(showCompleted.getValue());
            controller.dataProvider().refreshAll();
        });
        showCompleted.setValue(controller.settings().filter().showCompleted());

        includeParents = new Checkbox("Include Parents");
        includeParents.addValueChangeListener(event -> {
            controller.settings().filter().includeParents(includeParents.getValue());
            controller.dataProvider().refreshAll();
        });
        includeParents.setValue(controller.settings().filter().includeParents());
        
        ignoreScheduling = new Checkbox("Ignore Scheduling");
        ignoreScheduling.addValueChangeListener(event -> {
            controller.settings().ignoreScheduling(ignoreScheduling.getValue());
            controller.dataProvider().refreshAll();
        });
        ignoreScheduling.setValue(controller.settings().ignoreScheduling());

        innerJoinIncludedTags = new Checkbox("Inner join included tags");
        innerJoinIncludedTags.addValueChangeListener(event -> {
            controller.settings().filter().innerJoinIncludedTags(innerJoinIncludedTags.getValue());
            controller.dataProvider().refreshAll();
        });
        innerJoinIncludedTags.setValue(controller.settings().filter().innerJoinIncludedTags());

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

        this.add(topLayout, showOnlyBlocks, includeParents, showCompleted, ignoreScheduling, innerJoinIncludedTags, tagsToInclude, tagsToExclude);
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
        this.setColspan(topLayout, 4);
        this.setColspan(tagsToInclude, 4);
        this.setColspan(tagsToExclude, 4);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 2),
                new ResponsiveStep("1200px", 4));

        this.setWidthFull();
    }
}