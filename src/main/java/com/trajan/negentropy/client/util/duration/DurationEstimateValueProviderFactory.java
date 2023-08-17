package com.trajan.negentropy.client.util.duration;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@VaadinSessionScope
public class DurationEstimateValueProviderFactory<T extends HasTaskNodeData> {
    @Autowired private ClientDataController controller;
    @Autowired private UserSettings settings;

    public DurationEstimateValueProvider<T> get(
            DurationEstimateValueProvider.DurationType durationType) {
        return new DurationEstimateValueProvider<T>(controller, settings, durationType);
    }
}
