package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.client.controller.data.TaskProviderException;
import com.trajan.negentropy.client.util.CronConverter;
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
public class TaskEntryFormLayout extends AbstractTaskFormLayout {
    @Getter
    private final Binder<TaskEntry> binder = new BeanValidationBinder<>(TaskEntry.class);
    //    IntegerField priorityField;

    public TaskEntryFormLayout(ClientDataController controller, TaskEntry entry) {
        super(controller);
        binder.setBean(entry);

        configureFields();
        configureInteractions();
        configureBindings();
        configureLayout();
    }

    @Override
    protected void configureFields() {
        super.configureFields();

//        priorityField = new IntegerField("Priority");
//        priorityField.setHelperText("0 -> 9");
//        priorityField.setMaxWidth("70px");
//        priorityField.setMax(9);
//        binder.forField(priorityField)
//                .bind("importance");
    }

    @Override
    boolean isValid() {
        return binder.isValid();
    }

    @Override
    protected void configureBindings() {
        binder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(
                        entry -> entry.task().name(),
                        (entry, name) -> entry.task().name(name));

        binder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(
                        entry -> entry.task().duration(),
                        (entry, duration) -> entry.task().duration(duration));

        binder.forField(cronField)
                .withConverter(new CronConverter())
                .bind(
                        entry -> entry.node().cron(),
                        (entry, cronString) -> entry.node().cron(cronString));

        binder.forField(recurringCheckbox)
                .bind(
                        entry -> entry.node().recurring(),
                        (entry, recurring) -> entry.node().recurring(recurring));

        binder.forField(blockCheckbox)
                .bind(
                        entry -> entry.task().block(),
                        (entry, block) -> entry.task().block(block));

        binder.forField(descriptionArea)
                .bind(
                        entry -> entry.task().description(),
                        (entry, description) -> entry.task().description(description));

        tagComboBox = new CustomValueTagComboBox(controller, tag ->
                binder.getBean().task().tags().add(tag));

        binder.forField(tagComboBox)
                .bind(
                        entry -> entry.task().tags(),
                        (entry, tags) -> entry.task().tags(tags));

        saveButton.setEnabled(binder.isValid());
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });
    }

    @Override
    public Response hasValidTask() {
        return new Response(binder.isValid(), "Task in form is invalid");
    }

    @Override
    public Optional<Task> getTask() throws TaskProviderException {
        if (hasValidTask().success()) {
            return Optional.of(binder.getBean().node().child());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TaskNodeInfo getNodeInfo() {
        return binder.getBean().node();
    }
}
