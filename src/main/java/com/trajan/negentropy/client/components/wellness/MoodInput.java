package com.trajan.negentropy.client.components.wellness;

import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.util.BannerProvider;
import com.trajan.negentropy.model.Mood;
import com.trajan.negentropy.model.entity.Emotion;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Scope("prototype")
public class MoodInput extends HorizontalLayout {
    @Autowired private SessionServices services;
    @Autowired private BannerProvider bannerProvider;

    private final ComboBox<Emotion> emotionField = new ComboBox<>();

    @PostConstruct
    public void init() {
        this.addClassName("mood-input");
        this.setWidthFull();

        Mood lastMood = controller.services().mood().getLastMood();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

        emotionField.addClassName("emotion-field");
        emotionField.setLabel("What is your mood?");
        emotionField.setPlaceholder(lastMood.emotion() + " at " +  lastMood.timestamp().format(formatter));
        emotionField.setItemLabelGenerator(Emotion::toString);
        emotionField.setItems(Emotion.values());
        emotionField.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        emotionField.setSizeFull();

        emotionField.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                services.mood().persist(
                        new Mood(event.getValue()));
                emotionField.setPlaceholder(event.getValue().toString());
                bannerProvider.showRandomTenet();
                emotionField.clear();
            }
        });

        this.add(emotionField);
    }
}
