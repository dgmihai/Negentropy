package com.trajan.negentropy.view.util;

import com.vaadin.flow.component.button.Button;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public
class ToggleButton extends Button {
    private boolean toggled = true;

    public void toggle() {
        toggled = !toggled;
    }
}