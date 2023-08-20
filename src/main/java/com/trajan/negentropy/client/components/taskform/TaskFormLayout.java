package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.util.cron.CronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class TaskFormLayout extends AbstractTaskFormLayout {
    @Getter
    private final Binder<Task> taskBinder = new BeanValidationBinder<>(Task.class);
    @Getter
    private final Binder<TaskNodeDTO> nodeBinder = new BeanValidationBinder<>(TaskNodeDTO.class);

    @Getter
    @Setter
    protected Runnable onClose = () -> {};

    public TaskFormLayout(ClientDataController controller) {
        super(controller);

        taskBinder.setBean(new Task());
        nodeBinder.setBean(new TaskNodeDTO());

        configureAll();

        projectDurationField.setVisible(false);
    }

    @Override
    public TaskNode save() {
        TaskNode result = super.save();
        switch (OnSuccessfulSaveActions.get(onSaveSelect.getValue()).orElseThrow()) {
            case CLEAR -> this.onClear();
            case PERSIST -> {
                nodeBinder.setBean(result.toDTO());
                taskBinder.setBean(result.task());
            }
            case KEEP_TEMPLATE -> nameField.clear();
            case CLOSE -> onClose.run();
        }

        return result;
    }

    @Override
    public void onClear() {
        this.clearAllFields();
    }

    @Override
    protected void configureBindings() {
        taskBinder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(Task::name, Task::name);

        taskBinder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(Task::duration, Task::duration);

        nodeBinder.forField(cronField)
                .withConverter(new CronConverter())
                .bind(TaskNodeDTOData::cron, TaskNodeDTOData::cron);

        taskBinder.forField(descriptionArea)
                .bind(Task::description, Task::description);

        tagComboBox = new CustomValueTagComboBox(controller, tag ->
                taskBinder.getBean().tags().add(tag));

        taskBinder.forField(projectCheckbox)
                .bind(Task::project, Task::project);
        taskBinder.forField(requiredCheckbox)
                .bind(Task::required, Task::required);

        nodeBinder.forField(recurringCheckbox)
                .bind(TaskNodeDTOData::recurring, TaskNodeDTOData::recurring);

        nodeBinder.forField(projectDurationField)
                .withConverter(new DurationConverter())
                .bind(TaskNodeDTOData::projectDuration, TaskNodeDTOData::projectDuration);

        taskBinder.forField(tagComboBox)
                .bind(Task::tags, Task::tags);

        saveButton.setEnabled(taskBinder.isValid());
        taskBinder.addValueChangeListener(e ->
                saveButton.setEnabled(taskBinder.isValid()));

    }

    @Override
    public boolean isValid() {
        return taskBinder.isValid();
    }

    @Override
    public Task getTask() {
        return isValid()
                ? taskBinder.getBean()
                : null;
    }

    @Override
    public TaskNodeDTOData<?> getNodeInfo() {
        return nodeBinder.getBean();
    }
}