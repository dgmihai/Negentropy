package com.trajan.negentropy.client.components.wellness;

import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.util.BannerProvider;
import com.trajan.negentropy.model.Stressor;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Scope("prototype")
public class StressorInput extends HorizontalLayout {
    @Autowired private SessionServices services;
    @Autowired private BannerProvider bannerProvider;

    private final ComboBox<Stressor> stressorsBox = new ComboBox<>();

    @PostConstruct
    public void init() {
        this.addClassName("stressor-input");
        this.setWidthFull();

        stressorsBox.addClassName("emotion-field");
        stressorsBox.setLabel("What is on your mind, for better or worse?");
        stressorsBox.setItems(services.stressor().getAll()
                .toList());
        stressorsBox.setItemLabelGenerator(Stressor::name);
        stressorsBox.setAllowCustomValue(true);
        stressorsBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        stressorsBox.setSizeFull();

        stressorsBox.addCustomValueSetListener(event -> {
            Stressor stressor = services.stressor().persist(
                    new Stressor(event.getDetail()));
            stressorsBox.setItems(services.stressor().getAll()
                    .toList());
            save(stressor);
        });

        stressorsBox.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                save();
            }
        });

        this.add(stressorsBox);
    }

    private void save() {
        save(stressorsBox.getValue());
    }

    private void save(Stressor stressor) {
        services.stressor().record(stressor.id(), LocalDateTime.now());
        bannerProvider.showRandomTenet();
        stressorsBox.clear();
    }
}
