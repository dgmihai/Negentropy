package com.trajan.negentropy.client.util.duration;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskData;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.vaadin.flow.function.ValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RequiredArgsConstructor
public class DurationEstimateValueProvider<T extends HasTaskData> implements ValueProvider<T, String> {
    private final ClientDataController controller;
    private final UserSettings settings;
    @Setter
    private TimeFormat timeFormat = TimeFormat.DURATION;
    private final DurationType durationType;

    @Override
    public String apply(HasTaskData data) {
        if (data instanceof HasTaskNodeData hasNodeData) {
            return apply(hasNodeData.node());
        } else {
            return apply(data.task());
        }
    }

    private String format(Duration duration) {
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

    private String apply(Task task) {
        Duration duration = switch (durationType) {
            // TODO: Account for settings?
            case NET_DURATION -> controller.taskNetworkGraph().netDurations().get(task.id());
            case TASK_DURATION -> task.duration();
            case PROJECT_DURATION -> throw new IllegalArgumentException("Project duration not supported for tasks.");
        };

        if (duration == null) return "";
        if (duration.isZero()) return "-- -- --";
        
        return format(duration);
    }

    private String apply(TaskNode node) {
        Duration duration = switch (durationType) {
            // TODO: Account for settings?
            case NET_DURATION -> node.task().project() && node.projectDuration() != null
                    ? node.projectDuration()
                    : controller.taskNetworkGraph().netDurations().get(node.task().id());
            case TASK_DURATION -> node.task().duration();
            case PROJECT_DURATION -> node.projectDuration();
        };

        if (duration == null) return "";
        if (duration.isZero()) return "-- -- --";

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
