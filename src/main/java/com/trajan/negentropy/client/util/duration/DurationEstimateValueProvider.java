package com.trajan.negentropy.client.util.duration;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RequiredArgsConstructor
public class DurationEstimateValueProvider<T extends HasTaskData> implements ValueProvider<T, String> {
    private final TaskNetworkGraph taskNetworkGraph;

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

    public String format(Duration duration) {
        return DurationEstimateValueProvider.format(duration, timeFormat);
    }

    public static String format(Duration duration, TimeFormat timeFormat) {
        switch (timeFormat) {
            case TIME -> {
                return format(LocalTime.now().plus(duration));
            }
            case DURATION -> {
                return new DurationConverter().convertToPresentation(duration, null);
            }
            default -> {
                return "ERROR";
            }
        }
    }

    public static String format(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        return time.format(formatter);
    }

    private String apply(Task task) {
        Duration duration = switch (durationType) {
            // TODO: Account for settings?
            case NET_DURATION -> {
                try {
                    yield taskNetworkGraph.netDurationInfo().get().netTaskDurations().get(task.id());
                } catch (NullPointerException e) {
                    yield null;
                }
            }
            case TASK_DURATION -> task.duration();
//            case PROJECT_DURATION -> throw new IllegalArgumentException("Project duration not supported for tasks.");
        };

        if (duration == null) return "";
        if (duration.isZero()) return "-- -- --";
        
        return format(duration);
    }

    private String apply(TaskNode node) {
        Duration duration = switch (durationType) {
            // TODO: Account for settings?
            case NET_DURATION ->
                    (taskNetworkGraph.netDurationInfo().get() != null)
                            ? taskNetworkGraph.netDurationInfo().get().netNodeDurations().get(node.id())
                            : Duration.ZERO;
            case TASK_DURATION -> node.task().duration();
//            case PROJECT_DURATION -> node.projectDurationLimit();
        };

        if (duration == null) return "";
        if (duration.isZero()) return K.DURATION_PLACEHOLDER;

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
//        PROJECT_DURATION
    }
}
