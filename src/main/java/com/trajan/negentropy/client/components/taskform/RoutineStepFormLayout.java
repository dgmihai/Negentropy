package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;

@Getter
public class RoutineStepFormLayout extends AbstractTaskFormLayout {
    private final Binder<RoutineStep> binder = new BeanValidationBinder<>(RoutineStep.class);

    private final TaskNodeProvider taskNodeProvider = new TaskNodeProvider(controller) {
        @Override
        public Task getTask() {
            return isValid()
                    ? binder().getBean().task()
                    : null;
        }

        @Override
        public TaskNodeInfoData<?> getNodeInfo() {
            return isValid()
                    ? binder().getBean().node()
                    : null;
        }

        @Override
        public boolean isValid() {
            return binder.isValid();
        }
    };

    public RoutineStepFormLayout(UIController controller, RoutineStep step) {
        super(controller);
        binder.setBean(step);

        configureAll();

        saveAsLastCheckbox.setVisible(false);
    }

    @Override
    public TaskNodeProvider getTaskNodeProvider() {
        return taskNodeProvider;
    }

    @Override
    public void save() {
        Task result = taskNodeProvider.modifyTask(binder.getBean().task().id());

        if (result != null) {
            this.clear();
        }
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

        binder.forField(requiredCheckbox)
                .bind(
                        node -> node.task().required(),
                        (node, required) -> node.task().required(required));

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

        onSaveSelect.setVisible(false);
        saveAsLastCheckbox.setVisible(false);

        saveButton.setEnabled(binder.isValid());
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameField, durationField, tagComboBox, taskCheckboxLayout, descriptionArea, hr,
                projectComboBox, buttonLayout);
    }
}
