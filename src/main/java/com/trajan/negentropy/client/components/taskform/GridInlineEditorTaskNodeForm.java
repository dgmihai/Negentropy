package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.CronSpan;
import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.cron.ShortenedCronConverter;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import java.util.Optional;

public class GridInlineEditorTaskNodeForm<T extends HasTaskNodeData> extends AbstractTaskFormLayout {

    @Getter
    private final FormTaskNodeProvider taskNodeProvider = new FormTaskNodeProvider(controller) {
        @Override
        public Task getTask() {
            return binder.getBean().node().task();
        }

        @Override
        public TaskNodeInfoData<?> getNodeInfo() {
            return binder.getBean().node();
        }

        @Override
        public boolean isValid() {
            return super.isValid() && binder.isValid();
        }
    };

    @Getter
    private Binder<T> binder;
    private Class<T> clazz;

    protected TextField projectDurationLimit;
    protected IntegerField projectStepCountLimit;
    protected TimePicker projectEtaLimit;
    protected Checkbox recurringCheckbox;

    public GridInlineEditorTaskNodeForm(UIController controller, T data, Class<T> clazz) {
        super(controller);
        this.clazz = clazz;
        binder = new BeanValidationBinder<>(clazz);
        binder.setBean(data);

        configureAll();

        projectDurationLimit.setVisible(data.task().project());
        projectStepCountLimit.setVisible(data.task().project());
        projectEtaLimit.setVisible(data.task().project());
        saveAsLastCheckbox.setVisible(false);

        this.taskNodeProvider.afterSuccessfulSave(this::clear);
    }

    @Override
    public void save() {
        taskNodeProvider().modifyNode(binder.getBean().node().id());
    }

    @Override
    public FormTaskNodeProvider getTaskNodeProvider() {
        return taskNodeProvider;
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        recurringCheckbox = new Checkbox("Recurring");

        projectDurationLimit = new DurationTextField("Project ");
        projectDurationLimit.setValueChangeMode(ValueChangeMode.EAGER);
        projectDurationLimit.setClearButtonVisible(true);

        projectStepCountLimit = new IntegerField();
        projectStepCountLimit.setPlaceholder("Step Count Limit");
        projectStepCountLimit.setValueChangeMode(ValueChangeMode.EAGER);

        projectEtaLimit = new TimePicker();
        projectEtaLimit.setPlaceholder("Step ETA Limit");

        cronSpan = new CronSpan();
        cronSpan.cronField().setValueChangeMode(ValueChangeMode.EAGER);

        projectCheckbox.addValueChangeListener(e -> {
            projectDurationLimit.setVisible(e.getValue());
            projectStepCountLimit.setVisible(e.getValue());
            projectEtaLimit.setVisible(e.getValue());
        });
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

        binder.forField(cronSpan.cronField())
                .withConverter(new ShortenedCronConverter())
                .bind(
                        node -> node.node().cron(),
                        (node, cron) -> node.node().cron(cron));

        binder.forField(requiredCheckbox)
                .bind(
                        node -> node.task().required(),
                        (node, required) -> node.task().required(required));

        binder.forField(cleanupCheckbox)
                .bind(
                        node -> node.task().cleanup(),
                        (node, cleanup) -> node.task().cleanup(cleanup));

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

        tagComboBox = new CustomValueTagComboBox(controller,
                tag -> binder.getBean().task().tags().add(tag));

        binder.forField(tagComboBox)
                .bind(
                        node -> node.task().tags(),
                        (node, tags) -> node.task().tags(tags));

        binder.forField(projectDurationLimit)
                .withConverter(new DurationConverter())
                .bind(
                        node -> node.node().projectDurationLimit().orElse(null),
                        (node, projectDuration) -> node.node().projectDurationLimit(
                                Optional.ofNullable(projectDuration)));

        binder.forField(projectStepCountLimit)
                .bind(
                        node -> node.node().projectStepCountLimit().orElse(null),
                        (node, projectStepCount) -> node.node().projectStepCountLimit(
                                Optional.ofNullable(projectStepCount)));

        binder.forField(projectEtaLimit)
                .bind(
                        node -> node.node().projectEtaLimit().orElse(null),
                        (node, projectEta) -> node.node().projectEtaLimit(
                                Optional.ofNullable(projectEta)));

        onSaveSelect.setVisible(false);
        saveAsLastCheckbox.setVisible(false);

        saveButton.setEnabled(binder.isValid());
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });
    }

    @Override
    protected void configureLayout() {
        projectDurationLimit.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        cronSpan.setWidthFull();

        nodeInfoLayout = new HorizontalLayout(
                cronSpan, recurringCheckbox);

        nodeInfoLayout.setWidthFull();
        nodeInfoLayout.setAlignItems(Alignment.CENTER);
        nodeInfoLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        super.configureLayout();
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        this.add(nameField, taskInfoLayout, tagComboBox, descriptionArea, hr, nodeInfoLayout,
                projectComboBox, projectDurationLimit, projectStepCountLimit, projectEtaLimit, buttonLayout);
    }
}