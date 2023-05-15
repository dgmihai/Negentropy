package com.trajan.negentropy.client.tree.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

public class InlineIconButton extends Button {
    public InlineIconButton(Component icon) {
        super(icon);
        this.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    }

    public InlineIconButton(Component icon, Runnable onClick) {
        this(icon);
        this.addClickListener(event -> onClick.run());
    }
}
