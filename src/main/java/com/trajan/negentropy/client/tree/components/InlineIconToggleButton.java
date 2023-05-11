package com.trajan.negentropy.client.tree.components;

import com.vaadin.flow.component.icon.Icon;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Accessors(fluent = true)
@Getter
@Setter
public class InlineIconToggleButton extends InlineIconButton {
    private static final Logger logger = LoggerFactory.getLogger(InlineIconToggleButton.class);

    private static final String ACTIVATED = "primary-color-icon";
    private static final String DEACTIVATED = "unselected-color-icon";

    private Runnable onActivate = () -> {};
    private Runnable onDeactivate = () -> {};

    private boolean activated;

    public InlineIconToggleButton(Icon icon, Runnable onActivate, Runnable onDeactivate) {
        super(icon);

        if (onActivate != null) {
            this.onActivate = onActivate;
        }

        if (onDeactivate != null) {
            this.onDeactivate = onDeactivate;
        }

        activated = false;
        icon.addClassName(DEACTIVATED);

        this.addClickListener(event -> toggle());
    }

    public void activate() {
        getIcon().removeClassName(DEACTIVATED);
        getIcon().addClassName(ACTIVATED);
        logger.debug("ACTIVATE");
        activated = true;
        onActivate.run();
    }

    public void deactivate() {
        getIcon().removeClassName(ACTIVATED);
        getIcon().addClassName(DEACTIVATED);
        logger.debug("DEACTIVATE");
        activated = false;
        onDeactivate.run();
    }

    public void toggle() {
        if (activated) activate();
        else deactivate();
    }
}
