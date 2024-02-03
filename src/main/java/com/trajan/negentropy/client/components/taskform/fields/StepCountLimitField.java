package com.trajan.negentropy.client.components.taskform.fields;

import com.vaadin.flow.component.textfield.IntegerField;

public class StepCountLimitField extends IntegerField {
    public StepCountLimitField() {
        super();
        this.setPlaceholder("Step Count Limit");
        this.setClearButtonVisible(true);
    }
}
