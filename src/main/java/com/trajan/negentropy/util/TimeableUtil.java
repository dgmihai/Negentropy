package com.trajan.negentropy.util;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.interfaces.Timeable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@Benchmark(millisFloor = 10)
public class TimeableUtil {

    public interface TimeableAncestor<T extends Timeable> {
        List<T> descendants();
    }

    public static TimeableUtil get() {
        return new TimeableUtil();
    }

    public static LocalDateTime getNextDateTimeAfter(LocalDateTime since, LocalTime localTime) {
        if (localTime == null) return null;

        LocalDateTime result = localTime.atDate(since.toLocalDate());
        return (result.isBefore(since))
                ? getNextDateTimeAfter(since, result.plusDays(1))
                : result;
    }

    public static LocalDateTime getNextDateTimeAfter(LocalDateTime since, LocalDateTime localTime) {
        if (localTime == null) return null;
        return (localTime.isBefore(since))
                ? getNextDateTimeAfter(since, localTime.plusDays(1))
                : localTime;
    }

    public Duration getElapsedActiveDuration(Timeable timeable, LocalDateTime time) {
        try {
            Duration elapsedSuspendedDuration = timeable.elapsedSuspendedDuration() != null
                    ? timeable.elapsedSuspendedDuration()
                    : Duration.ZERO;
            return switch (timeable.status()) {
                case NOT_STARTED, LIMIT_EXCEEDED:
                    yield Duration.ZERO;
                case ACTIVE:
                    yield timeable.startTime() != null
                            ? Duration.between(timeable.startTime(), time)
                                .minus(elapsedSuspendedDuration)
                            : Duration.between(timeable.startTime(), time);
                case COMPLETED, POSTPONED, EXCLUDED:
                    yield timeable.startTime() != null
                            ? Duration.between(timeable.startTime(), timeable.finishTime())
                                .minus(elapsedSuspendedDuration)
                            : Duration.ZERO;
                case SUSPENDED, SKIPPED, DESCENDANT_ACTIVE:
                    yield timeable.startTime() != null
                            ? Duration.between(timeable.startTime(), timeable.lastSuspendedTime())
                                .minus(elapsedSuspendedDuration)
                            : Duration.ZERO;
            };
        } catch (NullPointerException e) {
            log.error("Error getting elapsed active duration for: " + timeable, e);
            return Duration.ZERO;
        }
    }

    public Duration getNestedElapsedActiveDuration(TimeableAncestor<?> parent, LocalDateTime time) {
        return getNestedElapsedActiveDuration(parent.descendants(), time);
    }

    private Duration getNestedElapsedActiveDuration(Collection<? extends Timeable> steps, LocalDateTime time) {
        return steps.stream()
                .map( s -> this.getElapsedActiveDuration(s, time))
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration getRemainingDuration(Timeable timeable, LocalDateTime time) {
        return getRemainingDuration(timeable, time, false);
    }

    public Duration getRemainingDuration(Timeable timeable, LocalDateTime time, boolean nonNegative) {
        return switch (timeable.status()) {
            case NOT_STARTED:
                yield timeable.duration();
            case ACTIVE, SUSPENDED, SKIPPED, DESCENDANT_ACTIVE:
                Duration remainingDuration = timeable.duration().minus(getElapsedActiveDuration(timeable, time));
                yield (nonNegative && remainingDuration.isNegative()) ? Duration.ZERO : remainingDuration;
            case COMPLETED, EXCLUDED, POSTPONED, LIMIT_EXCEEDED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingDurationIncludingLimitExceeded(Timeable timeable, LocalDateTime time) {
        return getRemainingDurationIncludingLimitExceeded(timeable, time, false);
    }

    public Duration getRemainingDurationIncludingLimitExceeded(Timeable timeable, LocalDateTime time, boolean nonNegative) {
        return switch (timeable.status()) {
            case NOT_STARTED, LIMIT_EXCEEDED:
                yield timeable.duration();
            case ACTIVE, SUSPENDED, SKIPPED, DESCENDANT_ACTIVE:
                Duration elaspedActiveDuration = getElapsedActiveDuration(timeable, time);
                elaspedActiveDuration = (nonNegative && elaspedActiveDuration.isNegative()) ? Duration.ZERO : elaspedActiveDuration;
                yield timeable.duration().minus(elaspedActiveDuration);
            case COMPLETED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingNestedDuration(RoutineStepData<?> ancestor, LocalDateTime time) {
        return this.getRemainingNestedDuration(ancestor, time, false);
    }

    public Duration getRemainingNestedDuration(RoutineStepData<?> step, LocalDateTime time, boolean allowNegative) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED, SKIPPED, DESCENDANT_ACTIVE:
                yield iterateToGetNestedDuration(step, time, allowNegative);
            case COMPLETED, EXCLUDED, POSTPONED, LIMIT_EXCEEDED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingNestedDurationIncludingLimitExceeded(RoutineStepData<?> step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED, SKIPPED, LIMIT_EXCEEDED, DESCENDANT_ACTIVE:
                yield iterateToGetNestedDuration(step, time, true);
            case COMPLETED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
        };    }

    private Duration iterateToGetNestedDuration(RoutineStepData<?> step, LocalDateTime time, boolean allowNegative) {
        return step.descendants().stream()
                .map( s -> {
                    Duration remaining = getRemainingDuration(s, time);
                    return remaining.isNegative() && !allowNegative ? Duration.ZERO : remaining;
                })
                .reduce(Duration.ZERO, Duration::plus);
    }

    public LocalDateTime currentTime() {
        return ServerClockService.now();
    }
}
