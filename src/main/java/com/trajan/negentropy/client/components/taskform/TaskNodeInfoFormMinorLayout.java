package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.CronSpan;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.ClearEventListener;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.cron.ShortenedCronConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

@Getter
public class TaskNodeInfoFormMinorLayout extends TaskFormLayout {
    protected Binder<TaskNodeDTO> nodeInfoBinder;
    protected HorizontalLayout nameFieldLayout;
    protected Button preexistingTaskSearchButton;

    public TaskNodeInfoFormMinorLayout(UIController controller) {
        super(controller);
        initTaskNodeDataProvider();
        initClearEventListener();

        nodeInfoBinder.setBean(new TaskNodeDTO());
        projectComboBox.setVisible(false);
        projectCheckbox.setVisible(false);
    }

    public TaskNodeInfoFormMinorLayout(UIController controller, Task task) {
        super(controller);
        initTaskNodeDataProvider();
        initClearEventListener();

        taskBinder.setBean(task);
        nodeInfoBinder.setBean(new TaskNodeDTO()
                .childId(task.id()));
        projectComboBox.setVisible(false);
        projectCheckbox.setVisible(false);
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

        preexistingTaskSearchButton = new Button("", VaadinIcon.FILE_SEARCH.create());
        preexistingTaskSearchButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        nameFieldLayout = new HorizontalLayout(nameField, preexistingTaskSearchButton);
        nameField.addValueChangeListener(e -> {
            String current = nameField.getValue();
            if (current.length() > 4) {
                long count = controller.services().query().fetchAllTasks(new TaskTreeFilter()
                        .name(nameField.getValue())).count();
                if (count > 0) {
                    preexistingTaskSearchButton.setEnabled(true);
                    preexistingTaskSearchButton.setText(String.valueOf(count));
                } else {
                    preexistingTaskSearchButton.setEnabled(false);
                    preexistingTaskSearchButton.setText("");
                }
            } else {
                preexistingTaskSearchButton.setEnabled(false);
                preexistingTaskSearchButton.setText("");
            }
        });

        recurringCheckbox = new Checkbox("Recurring");

        cronSpan = new CronSpan();
        cronSpan.cronField().setValueChangeMode(ValueChangeMode.EAGER);
    }

    @Override
    protected void configureBindings() {
        super.configureBindings();

        nodeInfoBinder = new BeanValidationBinder<>(TaskNodeDTO.class);

        nodeInfoBinder.forField(cronSpan.cronField())
                .withConverter(new ShortenedCronConverter())
                .bind(TaskNodeDTOData::cron, TaskNodeDTOData::cron);

        nodeInfoBinder.forField(recurringCheckbox)
                .bind(TaskNodeDTOData::recurring, TaskNodeDTOData::recurring);
    }

    @Override
    protected void configureInteractions() {
        super.configureInteractions();
        taskNodeProvider().afterSave(() -> nameField.focus());
    }

    @Override
    protected void configureLayout() {
        super.configureLayout();

        nameField.setWidthFull();
        nameFieldLayout.setSizeFull();
        nameFieldLayout.setPadding(false);

        cronSpan.setWidthFull();

        nodeInfoLayout = new HorizontalLayout(
                cronSpan, recurringCheckbox);

        nodeInfoLayout.setWidthFull();
        nodeInfoLayout.setAlignItems(Alignment.CENTER);
        nodeInfoLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameFieldLayout, taskInfoLayout, tagComboBox, descriptionArea, hr, nodeInfoLayout,
                buttonLayout);
    }
}