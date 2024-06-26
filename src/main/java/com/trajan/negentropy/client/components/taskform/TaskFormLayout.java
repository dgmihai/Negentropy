package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.taskform.fields.EffortConverter;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.HasRootNode;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;

import java.time.Duration;

public class TaskFormLayout extends AbstractTaskFormLayout {
    @Getter
    protected Binder<Task> taskBinder;
    private ChangeID watchedChangeId = null;

    @Getter
    protected FormTaskNodeProvider taskNodeProvider;

    @Override
    public FormTaskNodeProvider getTaskNodeProvider() {
        return taskNodeProvider;
    }

    protected void initTaskNodeDataProvider() {
        taskNodeProvider = new FormTaskNodeProvider(controller) {
            @Override
            public Task getTask() {
                return taskBinder.getBean();
            }

            @Override
            public TaskNodeInfoData<?> getNodeInfo() {
                return new TaskNodeDTO();
            }

            @Override
            public boolean isValid() {
                return taskBinder.isValid() && super.isValid();
            }

            @Override
            public void handleSave(DataMapResponse response) {
                if (response != null) {
                    switch (OnSuccessfulSaveActions.get(onSaveSelect.getValue()).orElseThrow()) {
                        case CLEAR -> clear();
                        case PERSIST -> {
                            TaskNode result = (TaskNode) response.changeRelevantDataMap().getFirst(changeId);
                            taskBinder.setBean(result.task());
                        }
                        case KEEP_TEMPLATE -> {
                            nameField.clear();
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

    public TaskFormLayout(UIController controller) {
        super(controller);

        configureAll();

        taskBinder.setBean(new Task());
    }

    public TaskFormLayout(UIController controller, Task task) {
        super(controller);

        configureAll();

        taskBinder.setBean(task);
    }

    protected void setDescription() {
        taskBinder.getBean().description(
                descriptionArea.getValue() != null
                        ? descriptionArea.getValue().trim()
                        : "");
    }
    @Override
    public void save() {
        this.setDescription();

        InsertLocation location = saveAsLastCheckbox.getValue() ?
                InsertLocation.LAST :
                InsertLocation.FIRST;

        Task taskReference = projectComboBox.getValue();
        TaskOrLinkID reference;

        if (taskReference == null) {
            HasRootNode taskNodeDisplay = controller.activeTaskNodeDisplay();
            TaskNode rootNode = taskNodeDisplay != null
                    ? controller.activeTaskNodeDisplay().rootNode().orElse(null)
                    : null;
            reference = rootNode == null ? null : rootNode.task().id();
        } else {
            reference = taskReference.id();
        }
        watchedChangeId = taskNodeProvider.createNode(reference, location);
    }

    @Override
    void configureBindings() {
        taskBinder = new BeanValidationBinder<>(Task.class);

        taskBinder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(Task::name,
                        (task, name) -> task.name(name.trim()));

        taskBinder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(Task::duration, (task, duration) ->
                        task.duration((duration == null)
                                ? Duration.ZERO
                                : duration));

        taskBinder.forField(effortSelect)
                .withConverter(new EffortConverter())
                .bind(Task::effort, Task::effort);

        taskBinder.forField(descriptionArea)
                .bind(Task::description,
                        (task, desc) -> task.description(desc != null ? desc.trim() : null));

        tagComboBox = new CustomValueTagComboBox(controller, (old, updated) ->
                taskBinder.getBean().tags(updated));

        taskBinder.forField(projectCheckbox)
                .bind(Task::project, Task::project);
        taskBinder.forField(requiredCheckbox)
                .bind(Task::required, Task::required);
        taskBinder.forField(cleanupCheckbox)
                .bind(Task::cleanup, Task::cleanup);

        taskBinder.forField(tagComboBox)
                .bind(Task::tags, Task::tags);

        onSaveSelect.setValue(controller.settings().onSuccessfulSaveAction().toString());
        onSaveSelect.addValueChangeListener(event -> controller.settings().onSuccessfulSaveAction(
                OnSuccessfulSaveActions.get(onSaveSelect.getValue()).orElseThrow()));

        saveButton.setEnabled(taskBinder.isValid());
        taskBinder.addValueChangeListener(e ->
                saveButton.setEnabled(taskBinder.isValid()));
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameField, taskInfoLayout, tagComboBox, descriptionArea, hr, projectComboBox,
                buttonLayout);
    }

}
