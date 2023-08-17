package com.trajan.negentropy.client.components.fields;

import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.vaadin.flow.component.textfield.TextField;

public class DurationTextField extends TextField {
    public DurationTextField() {
        super();
        setPlaceholder("Duration (?h ?m ?s)");
        setPattern(DurationConverter.DURATION_PATTERN);
        setErrorMessage("Required format: ?h ?m ?s (ex: 1m 30m, or 2h");
    }

    public DurationTextField(String prefix) {
        super();
        setPlaceholder(prefix + "Duration (?h ?m ?s)");
        setPattern(DurationConverter.DURATION_PATTERN);
        setErrorMessage("Required format: ?h ?m ?s (ex: 1m 30m, or 2h");
    }
}
