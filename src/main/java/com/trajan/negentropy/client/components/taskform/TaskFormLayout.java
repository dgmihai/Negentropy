package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.util.cron.CronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class TaskFormLayout extends AbstractTaskFormLayout {
    @Getter
    private final Binder<Task> taskBinder = new BeanValidationBinder<>(Task.class);
    @Getter
    private final Binder<TaskNodeDTO> nodeBinder = new BeanValidationBinder<>(TaskNodeDTO.class);

    public TaskFormLayout(ClientDataController controller) {
        super(controller);

        taskBinder.setBean(new Task());
        nodeBinder.setBean(new TaskNodeDTO());

        configureAll();

        projectDurationField.setVisible(false);
    }

    @Override
    boolean isValid() {
        return taskBinder.isValid();
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
                .bind(TaskNodeDTO::cron, TaskNodeDTO::cron);

        taskBinder.forField(descriptionArea)
                .bind(Task::description, Task::description);

        tagComboBox = new CustomValueTagComboBox(controller, tag ->
                taskBinder.getBean().tags().add(tag));

        taskBinder.forField(projectCheckbox)
                .bind(Task::project, Task::project);
        taskBinder.forField(requiredCheckbox)
                .bind(Task::required, Task::required);

        nodeBinder.forField(recurringCheckbox)
                .bind(TaskNodeDTO::recurring, TaskNodeDTO::recurring);

        nodeBinder.forField(projectDurationField)
                .withConverter(new DurationConverter())
                .bind(TaskNodeDTO::projectDuration, TaskNodeDTO::projectDuration);

        taskBinder.forField(tagComboBox)
                .bind(Task::tags, Task::tags);

        saveButton.setEnabled(taskBinder.isValid());
        taskBinder.addValueChangeListener(e ->
                saveButton.setEnabled(taskBinder.isValid()));

    }

    @Override
    public Response hasValidTask() {
        return new Response(taskBinder.isValid(), "Task in form is invalid");
    }

    @Override
    public Task getTask() {
        return hasValidTask().success()
                ? taskBinder.getBean()
                : null;
    }

    @Override
    public TaskNodeDTOData<?> getNodeInfo() {
        return nodeBinder.getBean();
    }

    @Override
    public void onSuccessfulSave(HasTaskNodeData data) {
        switch (OnSuccessfulSave.get(onSaveSelect.getValue()).orElseThrow()) {
            case CLEAR -> this.clear();
            case PERSIST -> {
                nodeBinder.setBean(data.node().toDTO());
                taskBinder.setBean(data.task());
            }
            case KEEP_TEMPLATE -> nameField.clear();
        }
    }

    @Override
    public void onFailedSave(Response response) {
        // No-op
    }
}