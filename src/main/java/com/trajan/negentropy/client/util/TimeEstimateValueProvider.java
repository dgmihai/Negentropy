package com.trajan.negentropy.client.util;

import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskRelationship;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.UIScope;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UIScope
public record TimeEstimateValueProvider(ToggleButton toggleButton) implements ValueProvider<TaskRelationship, String> {
    @Override
    public String apply(TaskRelationship taskRelationship) {
        TaskInfo taskInfo = taskRelationship.getTaskInfo();
        if (toggleButton.isToggled()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            return String.format(formatter.format(LocalDateTime.now().plus(taskInfo.getDuration())));
        } else {
            return new DurationConverter().convertToPresentation(taskInfo.getDuration(), null);
        }
    }
}
