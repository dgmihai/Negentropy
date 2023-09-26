package com.trajan.negentropy.client.util.duration;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.model.data.HasTaskData;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@VaadinSessionScope
public class DurationEstimateValueProviderFactory<T extends HasTaskData> {
    @Autowired private UIController controller;
    @Autowired private UserSettings settings;

    public DurationEstimateValueProvider<T> get(
            DurationEstimateValueProvider.DurationType durationType) {
        return new DurationEstimateValueProvider<T>(controller, settings, durationType);
    }
}
