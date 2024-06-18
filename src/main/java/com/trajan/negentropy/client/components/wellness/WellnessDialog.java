package com.trajan.negentropy.client.components.wellness;

import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.SessionServices;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@UIScope
public class WellnessDialog extends Dialog {
    @Autowired private SessionServices sessionServices;
    @Autowired private StressorInput stressorInput;
    @Autowired private MoodInput moodInput;
    private final UILogger log = new UILogger();

    @PostConstruct
    public void init() {
        this.setTitle();
        this.add(stressorInput, moodInput);
        this.setWidth("25rem");
    }

    private void setTitle() {
        String title = "How are you doing?";
        try {
            title = sessionServices.tenet().getRandom().toString();
        } catch (Exception e) {
            log.error("Failed to get random tenet for wellness dialog", e);
        }

        this.setHeaderTitle(title);
    }

    @Override
    public void open() {
        if (!this.isOpened()) {
            this.setTitle();
            super.open();
        }
    }
}
