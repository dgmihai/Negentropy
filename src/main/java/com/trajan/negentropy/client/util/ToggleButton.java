package com.trajan.negentropy.client.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@UIScope
public class ToggleButton extends Button {
    private boolean toggled = true;

    public void toggle() {
        toggled = !toggled;
    }
}