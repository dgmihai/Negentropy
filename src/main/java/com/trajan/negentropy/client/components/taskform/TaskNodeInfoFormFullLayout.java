package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

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

        projectDurationLimit = new DurationTextField("Project ");
        projectDurationLimit.setValueChangeMode(ValueChangeMode.EAGER);

        projectStepCountLimit = new IntegerField();
        projectStepCountLimit.setPlaceholder("Step Count Limit");
        projectStepCountLimit.setValueChangeMode(ValueChangeMode.EAGER);

        projectEtaLimit = new TimePicker();
        projectEtaLimit.setPlaceholder("Step ETA Limit");
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
                .bind(TaskNodeDTOData::projectDurationLimit, TaskNodeDTOData::projectDurationLimit);

        nodeInfoBinder.forField(projectStepCountLimit)
                .bind(TaskNodeDTOData::projectStepCountLimit, TaskNodeDTOData::projectStepCountLimit);

        nodeInfoBinder.forField(projectEtaLimit)
                .bind(TaskNodeDTOData::projectEtaLimit, TaskNodeDTOData::projectEtaLimit);
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
                projectDurationLimit, projectStepCountLimit, projectEtaLimit, projectComboBox, buttonLayout);
    }
}