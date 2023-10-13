package com.trajan.negentropy.client.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

@Route("notification-message")
@Slf4j
public class NotificationMessage extends Div {

    private static void notify(Notification notification) {
        try {
            notification.open();
        } catch (Exception e) {
            log.warn("Failed to open notification - is UI not available?");
            e.printStackTrace();
        }
    }

    private static Notification create(String message) {
        Notification notification = new Notification();
        notification.addClassName("notification-message");

        Span text = new Span(message);
        text.setMaxWidth("98%");
        text.setWhiteSpace(WhiteSpace.PRE_LINE);
        text.setSizeFull();

        Button closeButton = new Button(new Icon("lumo", "cross"));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.getElement().setAttribute("aria-label", "Close");
        closeButton.setWidth("0.5em");
        closeButton.addClickListener(event -> {
            notification.close();
        });

        HorizontalLayout layout = new HorizontalLayout(text, closeButton);
        layout.setWidthFull();

        notification.getElement().addEventListener("click", event -> {
            notification.close();
        });

        notification.add(layout);

        try {
            notification.open();
            notification.setPosition(Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            log.warn("Failed to open notification - is UI not available?");
        }

        return notification;
    }

    public static void result(String message) {
        if (message != null && !message.isBlank()) {
            int duration = 3 * 1000;

            Notification notification = create(message);

            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.setDuration(duration);
            notification.setPosition(Position.BOTTOM_CENTER);

            notify(notification);
        }
    }

    public static void banner(String message) {
        if (message != null && !message.isBlank()) {
            int duration = 10 * 1000;

            Notification notification = create(message);

            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            notification.setDuration(duration);
            notification.setPosition(Position.TOP_CENTER);

            notify(notification);
        }
    }

    public static void error(String message) {
        if (message != null && !message.isBlank()) {
            Notification notification = create(message);

            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setPosition(Position.TOP_CENTER);

            notify(notification);
        }
    }

    public static void error(Throwable t) {
        log.error(t.getMessage(), t);
        Notification notification = create(t.getLocalizedMessage());

        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

        notify(notification);
    }

}
