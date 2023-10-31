package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.logger.UILogger;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
public class UIAccessor {
    @Autowired private UI ui;

    public void acquire(Command command) {
        UILogger log = new UILogger();

        if (ui != null) {
            try {
                ui.access(command);
            } catch (UIDetachedException e) {
                log.warn("UI is detached", e);
            }
        } else {
            log.warn("UI instance not available");
            command.execute();
        }
    }
}
