package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.client.util.DurationConverter;
import com.trajan.negentropy.client.util.TaskProvider;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Accessors(fluent = true)
public abstract class AbstractTaskFormLayout extends FormLayout implements TaskProvider {
    private static final Logger logger = LoggerFactory.getLogger(TaskFormLayout.class);

    protected final TreeViewPresenter presenter;

    protected TextField nameField;
    protected TextField durationField;
    protected TextArea descriptionArea;
    protected CustomValueTagComboBox tagComboBox;
    protected HorizontalLayout buttonLayout;

    protected Button saveButton;
    protected Button clearButton;

    @Setter
    protected Runnable onClear = () -> { };

    @Setter
    protected Runnable onSave = () -> { };

    public AbstractTaskFormLayout(TreeViewPresenter presenter) {
        this.presenter = presenter;

        this.addClassName("task-entry-form");
    }

    protected void configureFields() {
        nameField = new TextField();
        nameField.setPlaceholder("Name *");
        nameField.setValueChangeMode(ValueChangeMode.EAGER);

        durationField = new TextField();
        durationField.setPlaceholder("Duration (?h ?m ?s)");
        durationField.setPattern(DurationConverter.DURATION_PATTERN);
        durationField.setErrorMessage("Required format: ?h ?m ?s (ex: 1m 30m, or 2h");

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
        descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        buttonLayout = new HorizontalLayout(
                saveButton,
                clearButton);

        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        tagComboBox.setPlaceholder("Tags");

        setColspan(descriptionArea, 2);
        setColspan(buttonLayout, 2);
        setColspan(tagComboBox, 2);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2));

        this.setWidthFull();
    }
}