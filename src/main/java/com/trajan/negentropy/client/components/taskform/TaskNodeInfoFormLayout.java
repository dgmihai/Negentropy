package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.CronTextField;
import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.ClearEventListener;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.cron.ShortenedCronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class TaskNodeInfoFormLayout extends TaskFormLayout {
    private Binder<TaskNodeDTO> nodeInfoBinder;

    @Override
    public TaskNodeProvider getTaskNodeProvider() {
        return taskNodeProvider;
    }

    public TaskNodeInfoFormLayout(UIController controller) {
        super(controller);
        initTaskNodeDataProvider();
        initClearEventListener();

        nodeInfoBinder.setBean(new TaskNodeDTO());
        projectDurationField.setVisible(false);
        projectComboBox.setVisible(true);
    }

    public TaskNodeInfoFormLayout(UIController controller, Task task) {
        super(controller);
        initTaskNodeDataProvider();
        initClearEventListener();

        taskBinder.setBean(task);
        nodeInfoBinder.setBean(new TaskNodeDTO()
                .childId(task.id()));
        projectDurationField.setVisible(false);
        projectComboBox.setVisible(true);
    }

    protected void initTaskNodeDataProvider() {
        taskNodeProvider = new TaskNodeProvider(controller) {
            @Override
            public Task getTask() {
                return taskBinder.getBean();
            }

            @Override
            public TaskNodeInfoData<?> getNodeInfo() {
                return nodeInfoBinder.getBean();
            }

            @Override
            public boolean isValid() {
                return taskBinder.isValid() && nodeInfoBinder.isValid();
            }
        };
    }

    protected void initClearEventListener() {
        clearEventListener = new ClearEventListener() {
            @Override
            protected void onClear() {
                clearAllFields();
                nodeInfoBinder.setBean(new TaskNodeDTO());
                taskBinder.setBean(new Task());
            }
        };
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
                projectDurationField.setVisible(e.getValue()));
    }

    @Override
    protected void configureBindings() {
        super.configureBindings();

        nodeInfoBinder = new BeanValidationBinder<>(TaskNodeDTO.class);

        nodeInfoBinder.forField(cronField)
                .withConverter(new ShortenedCronConverter())
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
        taskNodeProvider().afterSave(() -> nameField.focus());
    }

    @Override
    protected void configureLayout() {
        super.configureLayout();

        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.setWidthFull();

        nodeInfoLayout = new HorizontalLayout(
                cronField, recurringCheckbox);

        nodeInfoLayout.setWidthFull();
        nodeInfoLayout.setAlignItems(Alignment.CENTER);
        nodeInfoLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameField, taskInfoLayout, tagComboBox, descriptionArea, hr, nodeInfoLayout,
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
                case KEEP_TEMPLATE -> {
                    nameField.clear();
                    descriptionArea.clear();
                }
                case CLOSE -> {
                    this.clear();
                    onClose.run();
                }
            }
        }
    }
}