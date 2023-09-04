package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.fields.CronTextField;
import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.ClearEvents;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public abstract class AbstractTaskFormLayout extends ReadOnlySettableFormLayout
        implements TaskNodeProvider, ClearEvents {
    protected TextField nameField;
    protected TextField durationField;
    protected TextField cronField;
    protected CustomValueTagComboBox tagComboBox;
    protected Checkbox recurringCheckbox;
    protected Checkbox requiredCheckbox;
    protected Checkbox projectCheckbox;
    protected TextField projectDurationField;
    protected TextArea descriptionArea;
    protected ComboBox<Task> projectComboBox;
    protected FormLayout buttonLayout;
    protected HorizontalLayout taskCheckboxLayout;
    protected HorizontalLayout nodeCheckboxLayout;

    protected Button saveButton;
    protected Button clearButton;

    @Getter
    protected Checkbox saveAsLastCheckbox;
    protected Select<String> onSaveSelect;

    @Getter
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

        durationField = new DurationTextField();
        durationField.setValueChangeMode(ValueChangeMode.EAGER);
        durationField.setRequired(true);

        projectDurationField = new DurationTextField("Project ");
        projectDurationField.setValueChangeMode(ValueChangeMode.EAGER);

        cronField = new CronTextField();
        cronField.setValueChangeMode(ValueChangeMode.EAGER);

        recurringCheckbox = new Checkbox("Recurring");
        requiredCheckbox = new Checkbox("Required");
        projectCheckbox = new Checkbox("Project");
        projectCheckbox.addValueChangeListener(e ->
                projectDurationField.setEnabled(e.getValue()));

        descriptionArea = new TextArea();
        descriptionArea.setPlaceholder("Description");
        descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);

        saveButton = new Button();
        setSaveButtonText(null);

        projectComboBox = new ComboBox<>();
        TaskTreeFilter filter = new TaskTreeFilter();
        filter.options().add(TaskTreeFilter.ONLY_PROJECTS);
        List<Task> projects = controller.services().query().fetchAllTasks(filter)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toCollection(ArrayList::new));
        projectComboBox.setItems(projects);
        projectComboBox.setClearButtonVisible(true);
        projectComboBox.setItemLabelGenerator(Task::name);
        projectComboBox.setPlaceholder("Add directly to project");
        projectComboBox.setVisible(false);
        projectComboBox.addValueChangeListener(e -> setSaveButtonText(e.getValue()));

        clearButton = new Button("Cancel");
        saveAsLastCheckbox = new Checkbox("Save as last task");
        onSaveSelect = new Select<>();
        onSaveSelect.setItems(Arrays.stream(OnSuccessfulSaveActions.values())
                .map(OnSuccessfulSaveActions::toString)
                .collect(Collectors.toSet()));
    }

    private void setSaveButtonText(Task project) {
        if (project != null) {
            saveButton.setText("Save to " + project.name());
        } else {
//             TODO: This requires a callback to the active task node view
//            controller.activeTaskNodeView().rootNode().ifPresentOrElse(
//                    node -> saveButton.setText("Save to " + node.task().name()),
//                    () -> saveButton.setText("Save"));
            saveButton.setText("Save");
        }
    }

    public abstract TaskNode save();

    protected void configureInteractions() {
        saveButton.addClickListener(event -> this.save());
        clearButton.addClickListener(event -> this.clear());

        Shortcuts.addShortcutListener(this,
                this::save,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                this::clear,
                Key.ESCAPE);

        afterSave(() -> nameField.focus());
    }

    abstract void configureBindings();

    protected void configureLayout() {
        nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        durationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        onSaveSelect.addThemeVariants(SelectVariant.LUMO_SMALL);
        projectComboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);

        buttonLayout = new FormLayout(
                saveButton,
                clearButton,
                onSaveSelect,
                saveAsLastCheckbox);

        buttonLayout.setWidthFull();
        buttonLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(K.SHORT_WIDTH, 2));

        taskCheckboxLayout = new HorizontalLayout(
                requiredCheckbox,
                projectCheckbox);

        nodeCheckboxLayout = new HorizontalLayout(
                recurringCheckbox);

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
                new ResponsiveStep(K.SHORT_WIDTH, 2));

        this.setWidthFull();

        this.add(nameField, durationField, tagComboBox, taskCheckboxLayout, descriptionArea, hr, cronField, nodeCheckboxLayout,
                projectDurationField, projectComboBox, buttonLayout);
    }
}