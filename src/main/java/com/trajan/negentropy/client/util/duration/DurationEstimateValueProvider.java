package com.trajan.negentropy.client.util.duration;

import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.function.ValueProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

public class DurationEstimateValueProvider<T> implements ValueProvider<T, String> {

    private final QueryService queryService;
    private final Supplier<TimeFormat> timeFormatCallback;
    private final boolean netDuration;

    public DurationEstimateValueProvider(
            QueryService queryService, Supplier<TimeFormat> formatSupplier,
            boolean netDuration) {
        this.queryService = queryService;
        this.timeFormatCallback = formatSupplier;
        this.netDuration = netDuration;
    }

    @Override
    public String apply(T obj) {
        if (obj instanceof Task task) {
            return convert(task);
        } else if (obj instanceof TaskEntry entry) {
            return convert(entry.node().child());
        } else {
            return "Invalid source type.";
        }
    }

    private String convert(Task task) {
        TimeFormat timeFormat = timeFormatCallback.get();
        Duration duration = task.duration();
        if (netDuration) {
            duration = queryService.fetchNetTimeDuration(task.id());
        }
        if (duration.isZero()) {
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
}
