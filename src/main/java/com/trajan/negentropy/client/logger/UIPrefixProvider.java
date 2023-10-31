package com.trajan.negentropy.client.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
@Slf4j
public class UIPrefixProvider implements PrefixProvider {
    @Autowired private SessionPrefixProvider sessionPrefixProvider;

    @Getter private String prefix = "[] : ";

    @PostConstruct
    public void init() {
        UI ui = UI.getCurrent();
        prefix = "[" + ui.getUIId() + " : " + sessionPrefixProvider.sessionHash() + "]: ";
        log.info("Setting UI prefix '" + prefix);
    }
}
