package com.trajan.negentropy.client.tree.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;

public class InlineIconButton extends Button {
    public InlineIconButton(Icon icon) {
        super(icon);
        this.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    }

    public InlineIconButton(Icon icon, Runnable onClick) {
        this(icon);
        this.addClickListener(event -> onClick.run());
    }
}
