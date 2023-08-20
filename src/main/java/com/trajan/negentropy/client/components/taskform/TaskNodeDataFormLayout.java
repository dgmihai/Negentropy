package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
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
public class TaskNodeDataFormLayout<T extends HasTaskNodeData> extends AbstractTaskFormLayout {
    @Getter
    private Binder<T> binder;
    private Class<T> clazz;

    public TaskNodeDataFormLayout(ClientDataController controller, T node, Class<T> clazz) {
        super(controller);
        this.clazz = clazz;
        binder = new BeanValidationBinder<>(clazz);
        binder.setBean(node);

        configureAll();

        projectDurationField.setVisible(node.task().project());
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

        onSaveSelect.setValue(controller.settings().onSuccessfulSaveAction().toString());
        onSaveSelect.addValueChangeListener(event -> controller.settings().onSuccessfulSaveAction(
                OnSuccessfulSaveActions.valueOf(onSaveSelect.getValue())));

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

    @Override
    public TaskNode save() {
        return super.save();
//        switch (OnSuccessfulSaveActions.get(onSaveSelect.getValue()).orElseThrow()) {
//            case CLEAR -> this.clear();
//            case PERSIST -> {
//                try {
//                    T binderData = clazz.getConstructor().newInstance();
//                    binder.setBean(binderData);
//                } catch (Exception e) {
//                    NotificationError.show(e);
//                    this.clear();
//                }
//            }
//            case KEEP_TEMPLATE -> nameField.clear();
//        }
    }
}