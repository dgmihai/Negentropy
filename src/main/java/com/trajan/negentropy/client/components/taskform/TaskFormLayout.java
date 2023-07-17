package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskProviderException;
import com.trajan.negentropy.client.util.cron.CronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@Accessors(fluent = true)
public class TaskFormLayout extends AbstractTaskFormLayout {
    @Getter
    private final Binder<Task> taskBinder = new BeanValidationBinder<>(Task.class);
    @Getter
    private final Binder<TaskNodeInfo> nodeBinder = new BeanValidationBinder<>(TaskNodeInfo.class);

    public TaskFormLayout(ClientDataController controller) {
        super(controller);

        taskBinder.setBean(new Task());
        nodeBinder.setBean(new TaskNodeInfo());

        configureAll();
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
                .bind(TaskNodeInfo::cron, TaskNodeInfo::cron);

        taskBinder.forField(descriptionArea)
                .bind(Task::description, Task::description);

        tagComboBox = new CustomValueTagComboBox(controller, tag ->
                taskBinder.getBean().tags().add(tag));

        nodeBinder.forField(recurringCheckbox)
                .bind(TaskNodeInfo::recurring, TaskNodeInfo::recurring);
        taskBinder.forField(blockCheckbox)
                .bind(Task::block, Task::block);
        blockCheckbox.setValue(true);

        taskBinder.forField(projectCheckbox)
                .bind(Task::project, Task::project);

        nodeBinder.forField(projectDurationField)
                .withConverter(new DurationConverter())
                .bind(TaskNodeInfo::projectDuration, TaskNodeInfo::projectDuration);

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
    public Optional<Task> getTask() throws TaskProviderException {
        if (hasValidTask().success()) {
            return Optional.of(taskBinder.getBean());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TaskNodeInfo getNodeInfo() {
        return nodeBinder.getBean();
    }
}