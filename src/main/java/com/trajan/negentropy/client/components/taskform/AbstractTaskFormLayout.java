package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.ClearEventListener;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.SaveEventListener;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider.HasTaskNodeProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public abstract class AbstractTaskFormLayout extends ReadOnlySettableFormLayout
        implements HasTaskNodeProvider {
    protected TextField nameField;
    protected TextField durationField;
    protected CustomValueTagComboBox tagComboBox;
    protected Checkbox requiredCheckbox;
    protected Checkbox projectCheckbox;
    protected TextArea descriptionArea;
    protected ComboBox<Task> projectComboBox;
    protected FormLayout buttonLayout;
    protected HorizontalLayout taskInfoLayout;
    protected HorizontalLayout nodeInfoLayout;

    // Link Only
    protected TextField cronField;
    protected TextField projectDurationField;
    protected Checkbox recurringCheckbox;

    protected Button saveButton;
    protected Button clearButton;

    protected SaveEventListener<DataMapResponse> saveEventListener;

    @Getter
    protected Checkbox saveAsLastCheckbox;
    protected Select<String> onSaveSelect;

    @Getter
    protected UIController controller;

    public abstract TaskNodeProvider getTaskNodeProvider();

    @Getter
    protected ClearEventListener clearEventListener = new ClearEventListener() {
        @Override
        protected void onClear() {
            clearAllFields();
        }
    };

    @Getter
    @Setter
    protected Runnable onClose = () -> {};

    public AbstractTaskFormLayout(UIController controller) {
        this.controller = controller;
        this.addClassName("task-entry-form");
    }

    protected void configureAll() {
        configureFields();
        configureInteractions();
        configureBindings();
        configureLayout();
        initLayout();
    }

    protected void configureFields() {
        nameField = new TextField();
        nameField.setPlaceholder("Name *");
        nameField.setValueChangeMode(ValueChangeMode.EAGER);
        nameField.setRequired(true);

        durationField = new DurationTextField();
        durationField.setValueChangeMode(ValueChangeMode.EAGER);
        durationField.setRequired(true);

        requiredCheckbox = new Checkbox("Required");
        projectCheckbox = new Checkbox("Project");

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
//            controller.activeTaskNodeDisplay().rootNode().ifPresentOrElse(
//                    node -> saveButton.setText("Save to " + node.task().name()),
//                    () -> saveButton.setText("Save"));
            saveButton.setText("Save");
        }
    }

    public abstract void save();

    public void clear() {
        this.clearEventListener().clear();
    }

    protected void configureInteractions() {
        saveButton.addClickListener(event -> this.save());
        clearButton.addClickListener(event -> clearEventListener().clear());

        Shortcuts.addShortcutListener(this,
                this::save,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                clearEventListener()::clear,
                Key.ESCAPE);
    }

    abstract void configureBindings();

    protected void configureLayout() {
        nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        durationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        clearButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        onSaveSelect.addThemeVariants(SelectVariant.LUMO_SMALL);
        projectComboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);

        HorizontalLayout innerButtonLayout = new HorizontalLayout(saveButton, clearButton);
        HorizontalLayout innerButtonCheckboxes = new HorizontalLayout(onSaveSelect, saveAsLastCheckbox);
        innerButtonLayout.setWidthFull();
        innerButtonCheckboxes.setWidthFull();
        saveButton.setWidth("48%");
        clearButton.setWidth("48%");
        onSaveSelect.setWidthFull();
        saveAsLastCheckbox.setMinWidth("8em");
        innerButtonLayout.setWidthFull();
        innerButtonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        innerButtonCheckboxes.setJustifyContentMode(JustifyContentMode.BETWEEN);
        innerButtonCheckboxes.setAlignItems(Alignment.START);

        buttonLayout = new FormLayout(
                innerButtonLayout,
                innerButtonCheckboxes);
        buttonLayout.setWidthFull();

        durationField.setWidthFull();
        taskInfoLayout = new HorizontalLayout(
                durationField,
                requiredCheckbox,
                projectCheckbox);
        taskInfoLayout.setSpacing(false);
        taskInfoLayout.setAlignItems(Alignment.CENTER);
        taskInfoLayout.setWidthFull();
        taskInfoLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        tagComboBox.setPlaceholder("Tags");

        setColspan(descriptionArea, 2);
        setColspan(tagComboBox, 2);
        setColspan(buttonLayout, 2);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(K.SHORT_WIDTH, 2));

        this.setWidthFull();
    }

    protected abstract void initLayout();
}