package com.trajan.negentropy.client.tree.components;

import com.vaadin.flow.component.icon.Icon;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

@Accessors(fluent = true)
@Getter
@Setter
public class InlineIconToggleButton extends InlineIconButton {
    private static final Logger logger = LoggerFactory.getLogger(InlineIconToggleButton.class);

    private static final String ACTIVATED = "primary-color-icon";
    private static final String DEACTIVATED = "unselected-color-icon";

    private Runnable onActivate = () -> {};
    private Runnable onDeactivate = () -> {};

    private Icon activatedIcon;
    private Icon deactivatedIcon;

    private boolean activated = false;
    private BooleanSupplier isActivated = this::activated;

    public InlineIconToggleButton(Icon icon) {
        super(icon);

        icon.addClassName(DEACTIVATED);

        this.addClickListener(event -> toggle());
    }

    public InlineIconToggleButton(Icon activatedIcon, Icon deactivatedIcon) {
        this(deactivatedIcon);

        this.activatedIcon = activatedIcon;
        this.deactivatedIcon = deactivatedIcon;
    }

    public void onToggle(Runnable onToggle) {
        this.onActivate = onToggle;
        this.onDeactivate = onToggle;
    }

    public void activate() {
        getIcon().removeClassName(DEACTIVATED);
        getIcon().addClassName(ACTIVATED);
        activated = true;
        if (activatedIcon != null) {
            super.setIcon(activatedIcon);
        }
        onActivate.run();
    }

    public void deactivate() {
        getIcon().removeClassName(ACTIVATED);
        getIcon().addClassName(DEACTIVATED);
        activated = false;
        if (activatedIcon != null) {
            super.setIcon(deactivatedIcon);
        }
        onDeactivate.run();
    }

    public void toggle() {
        if (activated) deactivate();
        else activate();
    }
}
