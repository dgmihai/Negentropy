package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.model.Tenet;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@UIScope
@SpringComponent
public class BannerProvider {
    @Autowired private SessionServices services;
    @PostConstruct
    public void showRandomTenet() {
        Tenet random = services.tenet().getRandom();
        if (random != null) NotificationMessage.banner(random.body());
    }
}
