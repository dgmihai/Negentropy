package com.trajan.negentropy.view.util;

import com.trajan.negentropy.data.entity.Task;
import com.vaadin.flow.function.ValueProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record TimeEstimateValueProvider(ToggleButton toggleButton) implements ValueProvider<Task, String> {
    @Override
    public String apply(Task task) {
        if (toggleButton.isToggled()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            return String.format(formatter.format(LocalDateTime.now().plus(task.getDuration())));
        } else {
            return new DurationConverter().convertToPresentation(task.getDuration(), null);
        }
    }
}
