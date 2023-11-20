package com.trajan.negentropy.client.logger;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@SpringComponent
@VaadinSessionScope
@Slf4j
@Getter
public class SessionPrefixProvider implements PrefixProvider {
    private String prefix = "[] : ";
    private String body = "";
    private String browser = "";
    private String address = "";

    @PostConstruct
    public void init() {
        VaadinSession session = VaadinSession.getCurrent();
        address = session.getBrowser().getAddress();
        browser = session.getBrowser().getBrowserApplication();
        String additional = "";
        if (browser.contains("iPhone")) {
            additional = " (iOS)";
        } else if (browser.contains("Android")) {
            additional = " (Android)";
        } else if (browser.contains("Windows")) {
            additional = " (Win)";
        } else if (browser.contains("Linux")) {
            additional = " (Linux)";
        } else if (browser.contains("Mac")) {
            additional = " (Mac)";
        }
        body = address + additional;
        prefix = "[" + body + "]: ";
        log.info("Setting session prefix '" + prefix + "' for session on browser " + browser + " at address " + address);
    }

    public int sessionHash() {
        return Math.abs((browser + " " + address + " ").hashCode());
    }
}
