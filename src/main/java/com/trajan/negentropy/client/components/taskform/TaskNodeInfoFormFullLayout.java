package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.taskform.fields.DurationLimitField;
import com.trajan.negentropy.client.components.taskform.fields.StepCountLimitField;
import com.trajan.negentropy.client.components.taskform.fields.TimeLimitPickerField;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import java.util.Optional;

@Getter
public class TaskNodeInfoFormFullLayout extends TaskNodeInfoFormMinorLayout {
    protected TextField projectDurationLimit;
    protected IntegerField projectStepCountLimit;
    protected TimePicker projectEtaLimit;

    public TaskNodeInfoFormFullLayout(UIController controller) {
        super(controller);

        projectCheckbox.setVisible(true);
        projectComboBox.setVisible(true);
    }

    @Override
    protected void configureFields() {
        super.configureFields();

        projectDurationLimit = new DurationLimitField();
        projectDurationLimit.setValueChangeMode(ValueChangeMode.EAGER);

        projectStepCountLimit = new StepCountLimitField();
        projectStepCountLimit.setValueChangeMode(ValueChangeMode.EAGER);

        projectEtaLimit = new TimeLimitPickerField();
    }

    protected void setProjectParameterVisibility(boolean visibility) {
        projectDurationLimit.setVisible(visibility);
        projectStepCountLimit.setVisible(visibility);
        projectEtaLimit.setVisible(visibility);
    }

    @Override
    protected void configureBindings() {
        super.configureBindings();

        nodeInfoBinder.forField(projectDurationLimit)
                .withConverter(new DurationConverter())
                .bind(
                        node -> node.node().projectDurationLimit() != null
                                ? node.node().projectDurationLimit().orElse(null)
                                : null,
                        (node, projectDuration) -> node.node().projectDurationLimit(
                                Optional.ofNullable(projectDuration)));

        nodeInfoBinder.forField(projectStepCountLimit)
                .bind(
                        node -> node.node().projectStepCountLimit() != null
                                ? node.node().projectStepCountLimit().orElse(null)
                                : null,
                        (node, projectStepCount) -> node.node().projectStepCountLimit(
                                Optional.ofNullable(projectStepCount)));

        nodeInfoBinder.forField(projectEtaLimit)
                .bind(
                        node -> node.node().projectEtaLimit() != null
                                ? node.node().projectEtaLimit().orElse(null)
                                : null,
                        (node, projectEta) -> node.node().projectEtaLimit(
                                Optional.ofNullable(projectEta)));
    }

    @Override
    protected void configureLayout() {
        super.configureLayout();

        projectDurationLimit.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        projectStepCountLimit.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        projectEtaLimit.addThemeVariants(TimePickerVariant.LUMO_SMALL);
    }

    @Override
    protected void initLayout() {
        Hr hr = new Hr();
        this.setColspan(hr, 2);

        setProjectParameterVisibility(projectCheckbox.getValue());
        projectCheckbox.addValueChangeListener(e -> {
            setProjectParameterVisibility(e.getValue());
        });

        this.add(nameFieldLayout, taskInfoLayout, tagComboBox, descriptionArea, hr, nodeInfoLayout,
                projectComboBox, projectDurationLimit, projectStepCountLimit, projectEtaLimit, buttonLayout);
    }
}