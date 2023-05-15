package com.trajan.negentropy.client.tree.components;

import com.trajan.negentropy.client.K;
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

    private Runnable onActivate = () -> {};
    private Runnable onDeactivate = () -> {};

    private Icon activatedIcon;
    private Icon deactivatedIcon;

    private boolean activated = false;
    private BooleanSupplier isActivated = this::activated;

    public InlineIconToggleButton(Icon icon) {
        super(icon);

        icon.addClassName(K.ICON_COLOR_UNSELECTED);

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
        getIcon().removeClassName(K.ICON_COLOR_UNSELECTED);
        getIcon().addClassName(K.ICON_COLOR_PRIMARY);
        activated = true;
        if (activatedIcon != null) {
            super.setIcon(activatedIcon);
        }
        onActivate.run();
    }

    public void deactivate() {
        getIcon().removeClassName(K.ICON_COLOR_PRIMARY);
        getIcon().addClassName(K.ICON_COLOR_UNSELECTED);
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
