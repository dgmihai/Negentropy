package com.trajan.negentropy.client.util;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;

public class TimeButton extends ToggleButton {
    private Grid<?> grid;
    public TimeButton(Grid<?> grid) {
        super();
        this.setIcon(VaadinIcon.CLOCK.create());
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        this.addClickListener(event -> {
            this.toggle();
            if (this.isToggled()) {
                this.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                this.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            this.setIcon(VaadinIcon.CLOCK.create());
            grid.getDataProvider().refreshAll();
        });
    }
}
