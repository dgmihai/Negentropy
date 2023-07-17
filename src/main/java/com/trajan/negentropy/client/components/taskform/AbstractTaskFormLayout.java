package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskProvider;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public abstract class AbstractTaskFormLayout extends FormLayout implements TaskProvider {
    protected TextField nameField;
    protected TextField durationField;
    protected TextField cronField;
    protected CustomValueTagComboBox tagComboBox;
    protected Checkbox recurringCheckbox;
    protected Checkbox blockCheckbox;
    protected Checkbox projectCheckbox;
    protected TextField projectDurationField;
    protected TextArea descriptionArea;
    protected HorizontalLayout buttonLayout;
    protected HorizontalLayout taskCheckboxLayout;
    protected HorizontalLayout nodeCheckboxLayout;

    protected Button saveButton;
    protected Button clearButton;

    @Setter
    protected Runnable onClear = () -> { };

    @Setter
    protected Runnable onSave = () -> { };

    protected ClientDataController controller;

    public AbstractTaskFormLayout(ClientDataController controller) {
        this.controller = controller;
        this.addClassName("task-entry-form");
    }

    protected void configureAll() {
        configureFields();
        configureInteractions();
        configureBindings();
        configureLayout();
    }

    protected void configureFields() {
        nameField = new TextField();
        nameField.setPlaceholder("Name *");
        nameField.setValueChangeMode(ValueChangeMode.EAGER);
        nameField.setRequired(true);

        durationField = new TextField();
        durationField.setPlaceholder("Duration (?h ?m ?s)");
        durationField.setPattern(DurationConverter.DURATION_PATTERN);
        durationField.setErrorMessage("Required format: ?h ?m ?s (ex: 1m 30m, or 2h");
        durationField.setValueChangeMode(ValueChangeMode.EAGER);
        durationField.setRequired(true);

        projectDurationField = new TextField();
        projectDurationField.setPlaceholder("Project Duration (?h ?m ?s)");
        projectDurationField.setPattern(DurationConverter.DURATION_PATTERN);
        projectDurationField.setErrorMessage("Required format: ?h ?m ?s (ex: 1m 30m, or 2h");
        projectDurationField.setValueChangeMode(ValueChangeMode.EAGER);

        cronField = new TextField();
        cronField.setPlaceholder("Cron (S M H D M W)");
        cronField.setPattern(K.CRON_PATTERN);
        cronField.setValueChangeMode(ValueChangeMode.EAGER);

        recurringCheckbox = new Checkbox("Recurring");
        blockCheckbox = new Checkbox("Block");
        projectCheckbox = new Checkbox("Project");
        projectCheckbox.addValueChangeListener(e -> {
            projectDurationField.setVisible(e.getValue());
        });

        descriptionArea = new TextArea();
        descriptionArea.setPlaceholder("Description");
        descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);

        saveButton = new Button("Save");
        clearButton = new Button("Cancel");
    }

    protected void configureInteractions() {
        Runnable onSaveValid = () -> {
            if (this.isValid()) {
                onSave.run();
                nameField.focus();
            }
        };

        saveButton.addClickListener(event -> onSaveValid.run());
        clearButton.addClickListener(event -> onClear.run());

        Shortcuts.addShortcutListener(this,
                onSaveValid::run,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                onClear::run,
                Key.ESCAPE);

        clearButton.addClickListener(e -> onClear.run());
    }

    abstract boolean isValid();

    abstract void configureBindings();

    protected void configureLayout() {
        nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        durationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        buttonLayout = new HorizontalLayout(
                saveButton,
                clearButton);

        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        taskCheckboxLayout = new HorizontalLayout(
                blockCheckbox);

        nodeCheckboxLayout = new HorizontalLayout(
                recurringCheckbox,
                projectCheckbox);

        taskCheckboxLayout.setWidthFull();
        taskCheckboxLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        nodeCheckboxLayout.setWidthFull();
        nodeCheckboxLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        tagComboBox.setPlaceholder("Tags");

        Hr hr = new Hr();

        setColspan(descriptionArea, 2);
        setColspan(buttonLayout, 2);
        setColspan(hr, 2);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2));

        this.setWidthFull();

        this.add(nameField, durationField, tagComboBox, taskCheckboxLayout, descriptionArea, hr, cronField, nodeCheckboxLayout, projectDurationField,
                buttonLayout);
    }
}