package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider;
import com.trajan.negentropy.model.sync.TaskNodeLimits;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;

import java.time.Duration;
import java.time.LocalTime;

@SpringComponent
public class LimitValueProvider implements ValueProvider<TaskNodeLimits, String> {

    @Override
    public String apply(TaskNodeLimits limits) {
        if (limits.stepCount().isPresent()) {
            String result = limits.stepCount().get() + " steps";
            if (limits.duration().isPresent() || limits.time().isPresent()) {
                result += "+";
            }
            return result;
        } else if (limits.duration().isPresent() || limits.time().isPresent()) {
            Duration duration = limits.duration().orElse(null);
            LocalTime time = limits.time().orElse(null);

            LocalTime durationEta = (duration != null)
                    ? LocalTime.now().plus(limits.duration().get())
                    : null;
            LocalTime timeEta = (time != null)
                    ? limits.time().get()
                    : null;

            if (duration != null && time != null) {
                if (durationEta.isBefore(timeEta)) {
                    return format(limits.duration().get());
                } else {
                    return format(limits.time().get());
                }
            } else {
                return (duration != null) ? format(duration) : format(time);
            }
        } else {
            return "";
        }
    }

    private String format(Duration duration) {
        return DurationEstimateValueProvider.format(duration, TimeFormat.TIME);
    }

    private String format(LocalTime time) {
        return DurationEstimateValueProvider.format(time);
    }
}
