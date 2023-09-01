//package com.trajan.negentropy.client.util;
//
//import com.vaadin.flow.component.Text;
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.button.ButtonVariant;
//import com.vaadin.flow.component.html.Div;
//import com.vaadin.flow.component.icon.Icon;
//import com.vaadin.flow.component.notification.Notification;
//import com.vaadin.flow.component.notification.NotificationVariant;
//import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.router.Route;
//import lombok.extern.slf4j.Slf4j;
//
//@Route("notification-error")
//@Slf4j
//public class NotificationError extends Div {
//    public static Notification show(Throwable e) {
//        log.error(e.getMessage(), e);
//        return notify(e.getMessage());
//    }
//
//    public static Notification show(String message) {
//        log.error(message);
//        return notify(message);
//    }
//
//    public static Notification notify(String message) {
//        Notification notification = new Notification();
//        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//
//        Div text = new Div(new Text(message));
//
//        Button closeButton = new Button(new Icon("lumo", "cross"));
//        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//        closeButton.getElement().setAttribute("aria-label", "Close");
//        closeButton.addClickListener(event -> {
//            notification.close();
//        });
//
//        HorizontalLayout layout = new HorizontalLayout(text, closeButton);
//        layout.setAlignItems(Alignment.CENTER);
//
//        notification.add(layout);
//
//        try {
//            notification.open();
//            notification.setPosition(Notification.Position.TOP_CENTER);
//        } catch (Exception e) {
//            log.warn("Failed to open notification - is UI not available?");
//        }
//
//        return notification;
//    }
//
//}
