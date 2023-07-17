package com.trajan.negentropy.client.components.grid.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InlineIconButton extends Button {
    private static final Logger logger = LoggerFactory.getLogger(InlineIconButton.class);

    public InlineIconButton(Component icon) {
        super(icon);
        this.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    }

    public InlineIconButton(Component icon, Runnable onClick) {
        this(icon);
        this.addClickListener(event -> onClick.run());
    }
}
