package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.CronTextField;
import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.taskform.Bound.BoundToTaskAndNodeInfo;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.util.cron.CronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;


public class TaskNodeInfoFormLayout extends TaskFormLayout implements BoundToTaskAndNodeInfo {
    @Getter
    private Binder<TaskNodeDTO> nodeInfoBinder;

    public TaskNodeInfoFormLayout(ClientDataController controller) {
        super(controller);

        nodeInfoBinder.setBean(new TaskNodeDTO());
        projectDurationField.setEnabled(false);
        projectComboBox.setVisible(true);
    }

    public TaskNodeInfoFormLayout(ClientDataController controller, Task task) {
        super(controller);

        taskBinder.setBean(task);
        nodeInfoBinder.setBean(new TaskNodeDTO()
                .childId(task.id()));
        projectDurationField.setEnabled(false);
        projectComboBox.setVisible(true);
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        recurringCheckbox = new Checkbox("Recurring");

        projectDurationField = new DurationTextField("Project ");
        projectDurationField.setValueChangeMode(ValueChangeMode.EAGER);

        cronField = new CronTextField();
        cronField.setValueChangeMode(ValueChangeMode.EAGER);

        projectCheckbox.addValueChangeListener(e ->
                projectDurationField.setEnabled(e.getValue()));
    }

    @Override
    protected void configureBindings() {
        super.configureBindings();

        nodeInfoBinder = new BeanValidationBinder<>(TaskNodeDTO.class);

        nodeInfoBinder.forField(cronField)
                .withConverter(new CronConverter())
                .bind(TaskNodeDTOData::cron, TaskNodeDTOData::cron);

        nodeInfoBinder.forField(recurringCheckbox)
                .bind(TaskNodeDTOData::recurring, TaskNodeDTOData::recurring);

        nodeInfoBinder.forField(projectDurationField)
                .withConverter(new DurationConverter())
                .bind(TaskNodeDTOData::projectDuration, TaskNodeDTOData::projectDuration);
    }

    @Override
    protected void configureInteractions() {
        super.configureInteractions();
        afterSave(() -> nameField.focus());
    }

    @Override
    protected void configureLayout() {
        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        nodeCheckboxLayout = new HorizontalLayout(
                recurringCheckbox);

        nodeCheckboxLayout = new HorizontalLayout(
                recurringCheckbox);

        nodeCheckboxLayout.setWidthFull();
        nodeCheckboxLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        super.configureLayout();

        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameField, durationField, tagComboBox, taskCheckboxLayout, descriptionArea, hr, cronField, nodeCheckboxLayout,
                projectDurationField, projectComboBox, buttonLayout);
    }

    @Override
    public void handleSaveResult(TaskNode result) {
        if (result != null) {
            switch (OnSuccessfulSaveActions.get(onSaveSelect.getValue()).orElseThrow()) {
                case CLEAR -> this.clear();
                case PERSIST -> {
                    nodeInfoBinder.setBean(result.toDTO());
                    taskBinder.setBean(result.task());
                }
                case KEEP_TEMPLATE -> nameField.clear();
                case CLOSE -> {
                    this.clear();
                    onClose.run();
                }
            }
        }
    }

    @Override
    public void onClear() {
        this.clearAllFields();
    }
}