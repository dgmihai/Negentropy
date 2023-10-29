package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.StackWalker.StackFrame;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SpringComponent
@UIScope
public class UiAccessManager {
    private final UILogger log = new UILogger();

    @Getter
    @Autowired private UI ui;

    public void acquire(Command command) {
        if (ui != null) {
            String callStack = StackWalker.getInstance().walk(frames -> frames
                    .skip(1)
                    .filter(f -> f.getClassName().contains("trajan") &&
                            !f.getClassName().contains("UIAccessManager"))
                    .map(StackFrame::getClassName)
                    .reduce((a, b) -> a + "." + b)).get();
            log.debug(callStack + " attempting to acquire UI access");
            try {
                ui.access(command).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Failed to acquire UI access", e);
                NotificationMessage.error("Failed to acquire UI access");
            }
        }
    }
}
