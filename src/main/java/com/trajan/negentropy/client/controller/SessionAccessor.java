package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.logger.SessionLogger;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

@SpringComponent
@VaadinSessionScope
@Benchmark
public class SessionAccessor {

    public void acquire(UI ui, Command command) {
        SessionLogger log = new SessionLogger();
        log.debug("Acquiring Session");

        if (ui != null) {
            int retries = 3;
            while (retries > 0) {
                try {
                    ui.accessSynchronously(command);
                    break;
                } catch (UIDetachedException e) {
                    log.warn("Session is detached, retry " + retries, e);
                    retries--;
                }
            }
        } else {
            log.warn("Session instance not available");
            command.execute();
        }
    }
}
