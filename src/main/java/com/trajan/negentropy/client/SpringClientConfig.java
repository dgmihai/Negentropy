package com.trajan.negentropy.client;

import com.trajan.negentropy.client.components.ToolbarTabSheet;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@VaadinSessionScope
//@ComponentScan(value = "com.trajan.negentropy")
public class SpringClientConfig {
    @Autowired private ClientDataController controller;
    @Autowired private UserSettings settings;

    @Bean
    @Scope("prototype")
    public ToolbarTabSheet toolbarTabSheet() {
        return new ToolbarTabSheet(controller, settings);
    }
}
