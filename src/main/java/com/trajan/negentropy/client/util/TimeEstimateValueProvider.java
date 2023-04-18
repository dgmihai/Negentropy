package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.TaskEntry;
import com.trajan.negentropy.server.entity.Task;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.UIScope;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UIScope
public record TimeEstimateValueProvider(ToggleButton toggleButton) implements ValueProvider<TaskEntry, String> {
    @Override
    public String apply(TaskEntry entry) {
        Task task = entry.node().getReferenceTask();
        if (toggleButton.isToggled()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
            return LocalDateTime.now().plus(task.getDuration()).format(formatter);
        } else {
            return new DurationConverter().convertToPresentation(task.getDuration(), null);
        }
    }
}
