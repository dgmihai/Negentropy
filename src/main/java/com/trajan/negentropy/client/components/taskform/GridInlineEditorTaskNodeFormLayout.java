package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.CronTextField;
import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.taskform.Bound.BoundToTaskNodeData;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.cron.CronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class GridInlineEditorTaskNodeFormLayout<T extends HasTaskNodeData> extends AbstractTaskFormLayout
        implements TaskNodeProvider, BoundToTaskNodeData<T> {
    @Getter
    private Binder<T> binder;
    private Class<T> clazz;

    protected TextField cronField;
    protected TextField projectDurationField;
    protected Checkbox recurringCheckbox;

    public GridInlineEditorTaskNodeFormLayout(ClientDataController controller, T node, Class<T> clazz) {
        super(controller);
        this.clazz = clazz;
        binder = new BeanValidationBinder<>(clazz);
        binder.setBean(node);

        configureAll();

        projectDurationField.setEnabled(node.task().project());
        saveAsLastCheckbox.setVisible(false);
    }

    @Override
    public void save() {
        TaskNode result = modifyNode(binder.getBean().node().id());

        if (result != null) {
            this.clear();
        }
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        recurringCheckbox = new Checkbox("Recurring");

        projectDurationField = new DurationTextField("Project ");
        projectDurationField.setValueChangeMode(ValueChangeMode.EAGER);

        cronField = new CronTextField();
        cronField.setValueChangeMode(ValueChangeMode.EAGER);

        projectCheckbox.addValueChangeListener(e ->
                projectDurationField.setEnabled(e.getValue()));
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
    protected void configureLayout() {
        projectDurationField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        nodeCheckboxLayout = new HorizontalLayout(
                recurringCheckbox);

        nodeCheckboxLayout = new HorizontalLayout(
                recurringCheckbox);

        nodeCheckboxLayout.setWidthFull();
        nodeCheckboxLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        super.configureLayout();
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameField, durationField, tagComboBox, taskCheckboxLayout, descriptionArea, hr, cronField, nodeCheckboxLayout,
                projectDurationField, projectComboBox, buttonLayout);
    }
}