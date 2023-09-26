package com.trajan.negentropy.client.components.fields;

import com.trajan.negentropy.client.K;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

public class CronTextField extends TextField {
    public CronTextField() {
        super();
        setPlaceholder("Cron (H D M W)");
        setPattern(K.CRON_SHORT_PATTERN);
    }

    public CronTextField small() {
        setPlaceholder("H D M W");
        addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return this;
    }
}