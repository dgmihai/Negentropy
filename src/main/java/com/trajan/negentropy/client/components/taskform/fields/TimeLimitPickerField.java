package com.trajan.negentropy.client.components.taskform.fields;

import com.vaadin.flow.component.timepicker.TimePicker;

import java.time.Duration;

public class TimeLimitPickerField extends TimePicker {
    public TimeLimitPickerField() {
        super();
        this.setPlaceholder("Time Limit");
        this.setStep(Duration.ofMinutes(15));
        this.setClearButtonVisible(true);
    }
}
