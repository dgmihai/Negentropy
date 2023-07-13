package com.trajan.negentropy.client;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@UIScope
//@ComponentScan(value = "com.trajan.negentropy")
public class SpringClientConfig {
    @Autowired private ClientDataController controller;
    @Autowired private UserSettings settings;

    // TODO: Currently unused
}
