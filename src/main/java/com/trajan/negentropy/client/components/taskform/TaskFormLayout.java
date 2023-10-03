package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;

public class TaskFormLayout extends AbstractTaskFormLayout {
    @Getter
    protected Binder<Task> taskBinder;
    private ChangeID watchedChangeId = null;

    @Getter
    protected TaskNodeProvider taskNodeProvider = new TaskNodeProvider(controller) {
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
            return taskBinder.isValid();
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
                    case KEEP_TEMPLATE -> nameField.clear();
                    case CLOSE -> {
                        clear();
                        onClose.run();
                    }
                }
            }
            super.handleSave(response);
        }
    };

    @Override
    public TaskNodeProvider getTaskNodeProvider() {
        return taskNodeProvider;
    }

    public TaskFormLayout(UIController controller) {
        super(controller);

        configureAll();

        taskBinder.setBean(new Task());
    }

    @Override
    public void save() {
        InsertLocation location = saveAsLastCheckbox.getValue() ?
                InsertLocation.LAST :
                InsertLocation.FIRST;

        TaskNode rootNode = controller.activeTaskNodeDisplay().rootNode().orElse(null);
        TaskID rootTaskID = rootNode == null ? null : rootNode.task().id();
        watchedChangeId = taskNodeProvider.createNode(
                projectComboBox.getValue() == null
                        ? rootTaskID
                        : projectComboBox.getValue().id(),
                location);
    }

    @Override
    void configureBindings() {
        taskBinder = new BeanValidationBinder<>(Task.class);

        taskBinder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(Task::name, Task::name);

        taskBinder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(Task::duration, Task::duration);

        taskBinder.forField(descriptionArea)
                .bind(Task::description, Task::description);

        tagComboBox = new CustomValueTagComboBox(controller, tag ->
                taskBinder.getBean().tags().add(tag));

        taskBinder.forField(projectCheckbox)
                .bind(Task::project, Task::project);
        taskBinder.forField(requiredCheckbox)
                .bind(Task::required, Task::required);

        taskBinder.forField(tagComboBox)
                .bind(Task::tags, Task::tags);

        onSaveSelect.setValue(controller.settings().onSuccessfulSaveAction().toString());
        onSaveSelect.addValueChangeListener(event -> controller.settings().onSuccessfulSaveAction(
                OnSuccessfulSaveActions.valueOf(onSaveSelect.getValue())));

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
