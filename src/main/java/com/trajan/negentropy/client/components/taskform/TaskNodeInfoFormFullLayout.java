package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

@Getter
public class TaskNodeInfoFormFullLayout extends TaskNodeInfoFormMinorLayout {
    public TaskNodeInfoFormFullLayout(UIController controller) {
        super(controller);

        projectCheckbox.setVisible(true);
        projectComboBox.setVisible(true);
    }

    public TaskNodeInfoFormFullLayout(UIController controller, Task task) {
        super(controller);

        projectCheckbox.setVisible(true);
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

            @Override
            public void handleSave(DataMapResponse response) {
                if (response != null) {
                    switch (OnSuccessfulSaveActions.get(onSaveSelect.getValue()).orElseThrow()) {
                        case CLEAR -> clear();
                        case PERSIST -> {
                            TaskNode result = (TaskNode) response.changeRelevantDataMap().getFirst(changeId);
                            nodeInfoBinder.setBean(result.toDTO());
                            taskBinder.setBean(result.task());
                        }
                        case KEEP_TEMPLATE -> {
                            nameField.clear();
                            descriptionArea.clear();
                        }
                        case CLOSE -> {
                            clear();
                            onClose.run();
                        }
                    }
                }
                super.handleSave(response);
            }
        };
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        projectDurationField = new DurationTextField("Project ");
        projectDurationField.setValueChangeMode(ValueChangeMode.EAGER);

        projectCheckbox.addValueChangeListener(e ->
                projectDurationField.setVisible(e.getValue()));
    }

    @Override
    protected void configureBindings() {
        super.configureBindings();

        nodeInfoBinder.forField(projectDurationField)
                .withConverter(new DurationConverter())
                .bind(TaskNodeDTOData::projectDuration, TaskNodeDTOData::projectDuration);
    }

    @Override
    protected void configureLayout() {
        super.configureLayout();

        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameFieldLayout, taskInfoLayout, tagComboBox, descriptionArea, hr, nodeInfoLayout,
                projectDurationField, projectComboBox, buttonLayout);
    }
}