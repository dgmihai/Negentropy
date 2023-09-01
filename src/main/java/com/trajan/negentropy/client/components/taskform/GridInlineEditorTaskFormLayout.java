package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.util.cron.CronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class GridInlineEditorTaskFormLayout<T extends HasTaskNodeData> extends AbstractTaskFormLayout {
    @Getter
    private Binder<T> binder;
    private Class<T> clazz;

    public GridInlineEditorTaskFormLayout(ClientDataController controller, T node, Class<T> clazz) {
        super(controller);
        this.clazz = clazz;
        binder = new BeanValidationBinder<>(clazz);
        binder.setBean(node);

        configureAll();

        projectDurationField.setEnabled(node.task().project());
        saveAsLastCheckbox.setVisible(false);
    }

    @Override
    public TaskNode save() {
        TaskNode result = modifyNode(binder.getBean().node().id());

        if (result != null) {
            this.clear();
        }

        return result;
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

        binder.forField(cronField)
                .withConverter(new CronConverter())
                .bind(
                        node -> node.node().cron(),
                        (node, cron) -> node.node().cron(cron));

        binder.forField(requiredCheckbox)
                .bind(
                        node -> node.task().required(),
                        (node, required) -> node.task().required(required));

        binder.forField(recurringCheckbox)
                .bind(
                        node -> node.node().recurring(),
                        (node, recurring) -> node.node().recurring(recurring));

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

        binder.forField(projectDurationField)
                .withConverter(new DurationConverter())
                .bind(
                        node -> node.node().projectDuration(),
                        (node, projectDuration) -> node.node().projectDuration(projectDuration));

        onSaveSelect.setVisible(false);
        saveAsLastCheckbox.setVisible(false);

        saveButton.setEnabled(binder.isValid());
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });
    }

    @Override
    public boolean isValid() {
        return binder.isValid();
    }

    @Override
    public Task getTask() {
        return isValid()
                ? binder.getBean().task()
                : null;
    }

    @Override
    public TaskNodeDTOData<?> getNodeInfo() {
        return binder.getBean().node();
    }

    @Override
    public void onClear() { }
}