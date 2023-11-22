package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.K;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import java.util.function.Consumer;

public class IndeterminateToggleButton extends Button {
    private Boolean state;
    private final Consumer<Boolean> onStateChange;

    public IndeterminateToggleButton(String text, Consumer<Boolean> onStateChange) {
        super(text);
        this.onStateChange = onStateChange;
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
        setState(null);
        this.addClickListener(event -> this.toggle());
    }

    public void setState(Boolean state) {
        this.state = state;
        if (state == null) {
            this.addClassName(K.TOGGLEABLE_BUTTON_NULL);
            this.removeClassNames(K.TOGGLEABLE_BUTTON_TRUE, K.TOGGLEABLE_BUTTON_FALSE);
        } else if (state) {
            this.addClassName(K.TOGGLEABLE_BUTTON_TRUE);
            this.removeClassNames(K.TOGGLEABLE_BUTTON_NULL, K.TOGGLEABLE_BUTTON_FALSE);
        } else {
            this.addClassName(K.TOGGLEABLE_BUTTON_FALSE);
            this.removeClassNames(K.TOGGLEABLE_BUTTON_NULL, K.TOGGLEABLE_BUTTON_TRUE);
        }
    }

    private void toggle() {
        if (state == null) {
            this.setState(true);
        } else if (state) {
            this.setState(false);
        } else {
            this.setState(null);
        }
        onStateChange.accept(state);
    }
}
