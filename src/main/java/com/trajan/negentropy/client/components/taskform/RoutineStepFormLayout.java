package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.controller.UIController;
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

    private final FormTaskNodeProvider taskNodeProvider = new FormTaskNodeProvider(controller) {
        @Override
        public Task getTask() {
            return isValid()
                    ? binder.getBean().task()
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
            return super.isValid() && binder.isValid();
        }
    };

    public RoutineStepFormLayout(UIController controller, RoutineStep step) {
        super(controller);
        binder.setBean(step);

        configureAll();

        saveAsLastCheckbox.setVisible(false);

        this.taskNodeProvider.afterSuccessfulSave(this::clear);
    }

    @Override
    public FormTaskNodeProvider getTaskNodeProvider() {
        return taskNodeProvider;
    }

    @Override
    public void save() {
        taskNodeProvider.modifyTask();
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

        binder.forField(cleanupCheckbox)
                .bind(
                        node -> node.task().cleanup(),
                        (node, cleanup) -> node.task().cleanup(cleanup));

        binder.forField(descriptionArea)
                .bind(
                        node -> node.task().description(),
                        (node, description) -> node.task().description(description));

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

        this.add(nameField, taskInfoLayout, descriptionArea, hr,
                projectComboBox, buttonLayout);
    }
}
