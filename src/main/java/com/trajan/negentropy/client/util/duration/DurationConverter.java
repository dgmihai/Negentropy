package com.trajan.negentropy.client.util.duration;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationConverter implements Converter<String, Duration> {
    private static final Logger logger = LoggerFactory.getLogger(DurationConverter.class);

    public static final String DURATION_PATTERN = "^\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?\\s*$";
    private static final Pattern COMPILED_DURATION_PATTERN = Pattern.compile(DURATION_PATTERN);

    @Override
    public Result<Duration> convertToModel(String value, ValueContext context) {
        return DurationConverter.toModel(value);
    }

    public static Result<Duration> toModel(String value) {
        Matcher matcher = COMPILED_DURATION_PATTERN.matcher(value);
        if (!matcher.find()) {
            return Result.error("Invalid format");
        }
        String hoursString = matcher.group(1);
        String minutesString = matcher.group(2);
        String secondsString = matcher.group(3);

        long hours = hoursString != null ? Long.parseLong(hoursString) : 0;
        long minutes = minutesString != null ? Long.parseLong(minutesString) : 0;
        long seconds = secondsString != null ? Long.parseLong(secondsString) : 0;

        Duration duration = Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        return Result.ok(duration);
    }

    @Override
    public String convertToPresentation(Duration value, ValueContext context) {
        return DurationConverter.toPresentation(value);
    }

    public static String toPresentation(Duration value) {
        if (value == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();

        if (value.isNegative()) {
            value = value.abs();
            parts.add("+");
        }

        long hours = value.toHours();
        long minutes = (value.toMinutes() % 60);
        long seconds = (value.getSeconds() % 60);
        if (hours > 0) {
            parts.add(String.format("%d", hours) + "h");
        }
        if (minutes > 0) {
            parts.add(String.format("%d", minutes) + "m");
        }
        if (seconds > 0) {
            parts.add(String.format("%d", seconds) + "s");
        }
        return String.join(" ", parts);
    }
}


