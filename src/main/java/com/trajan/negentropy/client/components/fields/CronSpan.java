package com.trajan.negentropy.client.components.fields;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import lombok.Getter;

@Getter
public class CronSpan extends HorizontalLayout {
    private final CronTextField cronField = new CronTextField();

    public CronSpan() {
        super();

        Button cronDailyButton = new Button("D");
        cronDailyButton.addClickListener(e -> cronField.setValue("0 * * ?"));
        cronDailyButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        cronDailyButton.setWidth("1em");

        cronField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        cronField.setClearButtonVisible(true);
        cronField.setWidthFull();
        this.setWidthFull();
        this.add(cronField, cronDailyButton);
    }
}
