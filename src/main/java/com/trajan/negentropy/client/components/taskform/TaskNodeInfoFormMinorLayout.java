package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.YesNoDialog;
import com.trajan.negentropy.client.components.fields.CronSpan;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.ClearEventListener;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import java.util.Optional;

@Getter
public class TaskNodeInfoFormMinorLayout extends TaskFormLayout {
    protected Binder<TaskNodeDTO> nodeInfoBinder;
    protected HorizontalLayout nameFieldLayout;
    protected Button preexistingTaskSearchButton;

    protected Boolean confirmedMatchingTask = false;

    public TaskNodeInfoFormMinorLayout(UIController controller) {
        super(controller);
        initTaskNodeDataProvider();
        initClearEventListener();

        this.taskNodeProvider.async = false;

        nodeInfoBinder.setBean(new TaskNodeDTO());
        projectComboBox.setVisible(false);
        projectCheckbox.setVisible(false);
    }

    @Override
    protected void initTaskNodeDataProvider() {
        taskNodeProvider = new FormTaskNodeProvider(controller) {
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
                return super.isValid() && taskBinder.isValid() && nodeInfoBinder.isValid();
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
                            if (taskBinder.getBean().id() != null) {
                                taskBinder.setBean(taskBinder.getBean().copyWithoutID());
                            }
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

    private Optional<Task> getMatchingTask() {
        return controller.services().query().fetchAllTasks(new TaskTreeFilter()
                .exactName(nameField.getValue())).findFirst();
    }

    public void onNameValueChange() {
        confirmedMatchingTask = false;
        this.setSaveButtonTargetText(projectComboBox.getValue());
        String current = nameField.getValue();
        if (current.length() > 2) {
            Optional<Task> matchingTask = getMatchingTask();
            preexistingTaskSearchButton.setEnabled(matchingTask.isPresent());
        }
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        cancelButton.setText("Clear");

        preexistingTaskSearchButton = new Button("", VaadinIcon.EXCLAMATION.create());
        preexistingTaskSearchButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        preexistingTaskSearchButton.setEnabled(false);

        nameFieldLayout = new HorizontalLayout(nameField, preexistingTaskSearchButton);
        nameField.addValueChangeListener(e -> onNameValueChange());

        recurringCheckbox = new Checkbox("Recurring");

        cronSpan = new CronSpan();
        cronSpan.cronField().setValueChangeMode(ValueChangeMode.EAGER);
    }

    @Override
    public void save() {
        this.setDescription();

        if (getMatchingTask().isPresent() && !confirmedMatchingTask) {
            showTaskAlreadyExistsDialog(this::save);
        } else {
            super.save();
        }
    }

    @Override
    protected void configureBindings() {
        super.configureBindings();

        nodeInfoBinder = new BeanValidationBinder<>(TaskNodeDTO.class);

        nodeInfoBinder.addValueChangeListener(e ->
                saveButton.setEnabled(nodeInfoBinder.isValid()));

        nodeInfoBinder.forField(cronSpan.cronField())
                .withConverter(new ShortenedCronConverter())
                .bind(TaskNodeDTOData::cron, TaskNodeDTOData::cron);

        nodeInfoBinder.forField(recurringCheckbox)
                .bind(TaskNodeDTOData::recurring, TaskNodeDTOData::recurring);
    }

    @Override
    protected void configureInteractions() {
        super.configureInteractions();
        initTaskNodeDataProvider();
        taskNodeProvider().afterSave(() -> nameField.focus());

        preexistingTaskSearchButton().addClickListener(e -> {
            showTaskAlreadyExistsDialog(null);
        });
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

    public void showTaskAlreadyExistsDialog(Runnable saveCallback) {
        Dialog existingTaskDialog = new Dialog();
        existingTaskDialog.setHeaderTitle("Use Existing Task?");
        existingTaskDialog.add(new Span("A task named \"" + nameField.getValue() + "\" already exists. Would you like to use the old task, or overwrite it?"));

        Button useExistingTask = new Button("Use Existing");
        Button overwriteExistingTask = new Button("Overwrite");
        Button cancel = new Button("Cancel");

        useExistingTask.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        overwriteExistingTask.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Task match = controller.services().query().fetchAllTasks(new TaskTreeFilter()
                .exactName(nameField.getValue())).findFirst().orElseThrow();

        TaskFormLayout taskFormLayout = new TaskFormLayout(controller);
        taskFormLayout.remove(taskFormLayout.buttonLayout);
        taskFormLayout.taskBinder().setBean(match);
        taskFormLayout.setReadOnly(true);
        taskFormLayout.setWidthFull();
        existingTaskDialog.add(new Hr(), taskFormLayout);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        buttonLayout.setWidthFull();
        buttonLayout.add(useExistingTask, overwriteExistingTask, cancel);
        existingTaskDialog.getFooter().add(buttonLayout);
        useExistingTask.addClickListener(e1 -> {
            existingTaskDialog.close();
            confirmedMatchingTask = true;
            Optional<Task> matchingTaskOptional = controller.services().query().fetchAllTasks(new TaskTreeFilter()
                    .exactName(nameField.getValue())).findFirst();
            matchingTaskOptional.ifPresent(matchingTask -> {
                taskBinder.setBean(matchingTask);
                tagComboBox.setValue(controller.taskNetworkGraph().taskTagMap().get(matchingTask.id()));
                if (saveCallback != null) {
                    saveCallback.run();
                }
            });
            saveButton.setText("Add");
        });

        overwriteExistingTask.addClickListener(e2 -> {
            YesNoDialog overwriteConfirmationDialog = new YesNoDialog();
            overwriteConfirmationDialog.setHeaderTitle("Are you sure you want to overwrite the existing task?");
            overwriteConfirmationDialog.add(new Span("This action cannot be undone. Only takes effect on save."));
            overwriteConfirmationDialog.yes().addClickListener(e3 -> {
                existingTaskDialog.close();
                confirmedMatchingTask = true;
                Optional<Task> matchingTaskOptional = controller.services().query().fetchAllTasks(new TaskTreeFilter()
                        .exactName(nameField.getValue())).findFirst();
                matchingTaskOptional.ifPresent(matchingTask -> taskBinder.setBean(taskBinder.getBean()
                        .copyWithID(matchingTask.id())));
                if (saveCallback != null) {
                    saveCallback.run();
                } else {
                    saveButton.setText("Overwrite");
                }
            });

            overwriteConfirmationDialog.open();
        });

            cancel.addClickListener(e3 -> existingTaskDialog.close());
            existingTaskDialog.open();
    }
}