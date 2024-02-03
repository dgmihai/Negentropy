package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.fields.CronSpan;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTaskFormLayout extends ReadOnlySettableFormLayout
        implements HasTaskNodeProvider {
    @Getter protected TextField nameField;
    protected TextField durationField;
    protected CustomValueTagComboBox tagComboBox;
    protected Checkbox requiredCheckbox;
    protected Checkbox projectCheckbox;
    protected Checkbox cleanupCheckbox;
    protected TextArea descriptionArea;
    @Getter protected ComboBox<Task> projectComboBox;
    protected FormLayout buttonLayout;
    protected HorizontalLayout taskInfoLayout;
    protected HorizontalLayout nodeInfoLayout;

    // Link Only
    protected CronSpan cronSpan;
    protected Checkbox recurringCheckbox;

    @Getter protected Button saveButton;
    protected Button cancelButton;

    protected SaveEventListener<DataMapResponse> saveEventListener;

    @Getter protected Checkbox saveAsLastCheckbox;
    @Getter protected Select<String> onSaveSelect;

    @Getter
    protected UIController controller;

    public abstract FormTaskNodeProvider getTaskNodeProvider();

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
        nameField.setValueChangeMode(ValueChangeMode.LAZY);
        nameField.setRequired(true);
        nameField.setMinLength(3);
        nameField.setClearButtonVisible(true);
        this.addAttachListener(e -> nameField.focus());

        durationField = new DurationTextField();
        durationField.setValueChangeMode(ValueChangeMode.EAGER);
        durationField.setClearButtonVisible(true);
        durationField.setRequired(true);

        requiredCheckbox = new Checkbox();
        requiredCheckbox.setTooltipText("Required");
        Icon requiredIcon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
        requiredIcon.addClassName(K.ICON_COLOR_PRIMARY);
        requiredCheckbox.setLabelComponent(requiredIcon);

        projectCheckbox = new Checkbox();
        projectCheckbox.setTooltipText("Project");
        Icon projectIcon = VaadinIcon.FILE_TREE_SUB.create();
        projectIcon.addClassName(K.ICON_COLOR_PRIMARY);
        projectCheckbox.setLabelComponent(projectIcon);

        cleanupCheckbox = new Checkbox();
        cleanupCheckbox.setTooltipText("Cleanup");
        Icon recycleIcon = VaadinIcon.RECYCLE.create();
        recycleIcon.addClassName(K.ICON_COLOR_PRIMARY);
        cleanupCheckbox.setLabelComponent(recycleIcon);

        descriptionArea = new TextArea();
        descriptionArea.setPlaceholder("Description");
        descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);
        descriptionArea.setClearButtonVisible(true);

        saveButton = new Button();
        setSaveButtonTargetText(null);

        projectComboBox = new ComboBox<>();
        TaskTreeFilter filter = new TaskTreeFilter()
                .onlyStarred(true);
        List<Task> projects = controller.services().query().fetchAllTasks(filter)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toCollection(ArrayList::new));
        projectComboBox.setItems(projects);
        projectComboBox.setClearButtonVisible(true);
        projectComboBox.setItemLabelGenerator(Task::name);
        projectComboBox.setPlaceholder("Add directly to starred task");
        projectComboBox.setVisible(false);
        projectComboBox.addValueChangeListener(e -> setSaveButtonTargetText(e.getValue()));

        cancelButton = new Button("Cancel");
        saveAsLastCheckbox = new Checkbox("Save as last task");
        onSaveSelect = new Select<>();
        onSaveSelect.setItems(Arrays.stream(OnSuccessfulSaveActions.values())
                .map(OnSuccessfulSaveActions::toString)
                .collect(Collectors.toSet()));
    }

    protected void setSaveButtonTargetText(Task project) {
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
        cancelButton.addClickListener(event -> clearEventListener().clear());

        Shortcuts.addShortcutListener(this,
                this::save,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                clearEventListener()::clear,
                Key.ESCAPE);
    }

    abstract void configureBindings();

    protected HorizontalLayout getInnerButtonLayout() {
        HorizontalLayout innerButtonLayout = new HorizontalLayout(saveButton, cancelButton);
        innerButtonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        saveButton.setWidth("48%");
        cancelButton.setWidth("48%");
        onSaveSelect.setWidthFull();
        saveAsLastCheckbox.setMinWidth("8em");
        innerButtonLayout.setWidthFull();
        return innerButtonLayout;
    }

    protected void configureLayout() {
        nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        durationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        onSaveSelect.addThemeVariants(SelectVariant.LUMO_SMALL);
        projectComboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);

        HorizontalLayout innerButtonCheckboxes = new HorizontalLayout(onSaveSelect, saveAsLastCheckbox);

        innerButtonCheckboxes.setWidthFull();
        innerButtonCheckboxes.setJustifyContentMode(JustifyContentMode.BETWEEN);
        innerButtonCheckboxes.setAlignItems(Alignment.START);

        buttonLayout = new FormLayout(
                getInnerButtonLayout(),
                innerButtonCheckboxes);
        buttonLayout.setWidthFull();

        durationField.setWidthFull();
        taskInfoLayout = new HorizontalLayout(
                durationField,
                requiredCheckbox,
                projectCheckbox,
                cleanupCheckbox);
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
                new ResponsiveStep(K.SHORT_SCREEN_WIDTH, 2));

        this.setWidthFull();
    }

    protected abstract void initLayout();

    public abstract class FormTaskNodeProvider extends TaskNodeProvider {
        public FormTaskNodeProvider(UIController controller) {
            super(controller);
        }

        @Override
        public boolean isValid() {
            return !nameField.isEmpty();
        }
    }
}