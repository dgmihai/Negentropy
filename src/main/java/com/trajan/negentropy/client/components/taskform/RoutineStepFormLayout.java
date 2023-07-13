package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskProviderException;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@Accessors(fluent = true)
public class RoutineStepFormLayout extends AbstractTaskFormLayout {

    @Getter
    private Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    public RoutineStepFormLayout(ClientDataController controller, RoutineStep step) {
        super(controller);
        binder.setBean(step);

        recurringCheckbox.removeFromParent();
        cronField.removeFromParent();
        this.addClassName("routine-step-form");
    }

    @Override
    boolean isValid() {
        return binder.isValid();
    }

    @Override
    void configureBindings() {
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
            return Optional.of(binder.getBean().task());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TaskNodeInfo getNodeInfo() {
        return new TaskNodeInfo();
    }
}
