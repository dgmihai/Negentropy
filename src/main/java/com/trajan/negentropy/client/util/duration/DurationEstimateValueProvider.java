package com.trajan.negentropy.client.util.duration;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.vaadin.flow.function.ValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RequiredArgsConstructor
public class DurationEstimateValueProvider<T extends HasTaskNodeData> implements ValueProvider<T, String> {
    private final ClientDataController controller;
    private final UserSettings settings;
    @Setter
    private TimeFormat timeFormat = TimeFormat.DURATION;
    private final DurationType durationType;

    @Override
    public String apply(HasTaskNodeData data) {
        return apply(data.node());
    }

    private String apply(TaskNode node) {
        Duration duration = switch (durationType) {
            // TODO: Account for settings?
            case NET_DURATION -> controller.services().query().fetchTimeEstimate(node.task().id(), null);
            case TASK_DURATION -> node.task().duration();
            case PROJECT_DURATION -> node.projectDuration();
        };

        if (duration == null || duration.isZero()) {
            return "-- -- --";
        }

        switch (timeFormat) {
            case TIME -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
                return LocalDateTime.now().plus(duration).format(formatter);
            }
            case DURATION -> {
                return new DurationConverter().convertToPresentation(duration, null);
            }
            default -> {
                return "ERROR";
            }
        }
    }

    public void toggleFormat() {
        switch (timeFormat) {
            case TIME -> timeFormat = TimeFormat.DURATION;
            case DURATION -> timeFormat = TimeFormat.TIME;
        }
    }

    public enum DurationType {
        NET_DURATION,
        TASK_DURATION,
        PROJECT_DURATION
    }
}
