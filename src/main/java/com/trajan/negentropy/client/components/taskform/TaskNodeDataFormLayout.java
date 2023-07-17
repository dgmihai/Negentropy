package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskNodeData;
import com.trajan.negentropy.client.controller.data.TaskProviderException;
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
public class TaskNodeDataFormLayout<T extends TaskNodeData> extends AbstractTaskFormLayout {

    @Getter
    private Binder<T> binder;

    public TaskNodeDataFormLayout(ClientDataController controller, T node, Class<T> clazz) {
        super(controller);
        binder = new BeanValidationBinder<>(clazz);
        binder.setBean(node);

        configureAll();

        projectDurationField.setVisible(node.task().project());

        this.addClassName("routine-node-form");
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
                        node -> node.task().name(),
                        (node, name) -> node.task().name(name));

        binder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(
                        node -> node.task().duration(),
                        (node, duration) -> node.task().duration(duration));

        binder.forField(blockCheckbox)
                .bind(
                        node -> !node.task().block(),
                        (node, notBlock) -> node.task().block(!notBlock));

        binder.forField(projectCheckbox)
                .bind(
                        node -> node.task().project(),
                        (node, project) -> node.task().project(project));

        binder.forField(descriptionArea)
                .bind(
                        node -> node.task().description(),
                        (node, description) -> node.task().description(description));

        tagComboBox = new CustomValueTagComboBox(controller, tag ->
                binder.getBean().task().tags().add(tag));

        binder.forField(tagComboBox)
                .bind(
                        node -> node.task().tags(),
                        (node, tags) -> node.task().tags(tags));

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
