package com.trajan.negentropy.client.util;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

@Route("notification-message")
@Slf4j
public class NotificationMessage extends Div {

    private static Notification create(String message) {
        Notification notification = new Notification();
        Div text = new Div(new Text(message));

        notification.getElement().addEventListener("click", event -> {
            notification.close();
        });

        notification.add(text);
        notification.setPosition(Position.TOP_CENTER);

        return notification;
    }

    private static Notification show(Notification notification) {
        try {
            notification.open();
        } catch (Exception e) {
            log.warn("Failed to open notification - is UI not available?");
        }

        return notification;
    }

    public static Notification result(String message) {
        int duration = 3 * 1000;

        Notification notification = create(message);

        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setDuration(duration);

        return show(notification);
    }

    public static Notification banner(String message) {
        int duration = 10 * 1000;

        Notification notification = create(message);

        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        notification.setDuration(duration);

        return show(notification);
    }

}
