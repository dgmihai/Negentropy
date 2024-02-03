package com.trajan.negentropy.client.util;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;

@SpringComponent
public class TimeableStatusValueProvider implements ValueProvider<TimeableStatus, String> {
    @Override
    public String apply(TimeableStatus timeableStatus) {
        return timeableStatus.toString();
    }
}