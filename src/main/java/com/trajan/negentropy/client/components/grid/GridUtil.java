package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.client.K;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.vaadin.lineawesome.LineAwesomeIcon;

public class GridUtil {
    public static final String DURATION_COL_WIDTH = "70px";
    public static final String ICON_COL_WIDTH_S = "31px";
    public static final String ICON_COL_WIDTH_L = "35px";
    public static final String ICON_COL_WIDTH_XL = "38px";

    public static String inlineLineAwesomeIconLitExpression(LineAwesomeIcon lineAwesomeIcon, String attributes) {
        return "<span class=\"grid-icon-lineawesome\" " + attributes + " style=\"-webkit-mask-position: var(--mask-position); display: block; -webkit-mask-repeat: var(--mask-repeat); vertical-align: middle; --mask-repeat: no-repeat; background-color: currentcolor; --_size: var(--lumo-icon-size-m); flex: 0 0 auto; width: var(--_size); --mask-position: 50%; -webkit-mask-image: var(--mask-image); " +
                "--mask-image: url('line-awesome/svg/" + lineAwesomeIcon.getSvgName() + ".svg'); height: var(--_size);\"></span>";
    }

    public static String inlineVaadinIconLitExpression(String iconName, String attributes) {
        return "<vaadin-icon class=\"grid-icon-vaadin\" icon=\"vaadin:" + iconName + "\" " +
                "@click=\"${onClick}\" " +
                attributes + "></vaadin-icon>";
    }

    public static Icon headerIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.setSize(K.INLINE_ICON_SIZE);
        icon.addClassName(K.ICON_COLOR_GRAYED);
        return icon;
    }

    public static Icon headerIconPrimary(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.setSize(K.INLINE_ICON_SIZE);
        icon.addClassName(K.ICON_COLOR_PRIMARY);
        return icon;
    }
}
