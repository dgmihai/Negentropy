package com.trajan.negentropy.client;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.logger.UILogger;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@UIScope
//@ComponentScan(value = "com.trajan.negentropy")
public class SpringClientConfig {
    private final UILogger log = new UILogger();

    @Autowired private UIController controller;
    @Autowired private UserSettings settings;

    @PostConstruct
    public void init() {
        log.debug("Initializing SpringClientConfig");
    }
}
