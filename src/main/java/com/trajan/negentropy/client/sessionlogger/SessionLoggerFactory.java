package com.trajan.negentropy.client.sessionlogger;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@VaadinSessionScope
@Slf4j
public class SessionLoggerFactory {
    @Autowired private VaadinSession session;
    private String sessionPrefix = "[] : ";

    @PostConstruct
    public void init() {
        String browser = session.getBrowser().getBrowserApplication();
        String address = session.getBrowser().getAddress();
        sessionPrefix = "[" + Math.abs((browser + " " + address + " ").hashCode()) + "]: ";
        log.info("Setting session prefix '" + sessionPrefix + "' for session on browser " + browser + " at address " + address);
    }

    public SessionLogger getLogger(Class<?> clazz) {
        return new SessionLogger(sessionPrefix, clazz);
    }
}
