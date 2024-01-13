package com.trajan.negentropy.client.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

@Getter
public class YesNoDialog extends Dialog {
    private Button yes;
    private Button no;

    public YesNoDialog() {
        yes = new Button("Yes");
        yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        no = new Button("No");
        no.addClickListener(e -> this.close());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setJustifyContentMode(JustifyContentMode.BETWEEN);
        buttons.setWidthFull();
        buttons.add(yes, no);

        this.getFooter().add(buttons);
    }
}
