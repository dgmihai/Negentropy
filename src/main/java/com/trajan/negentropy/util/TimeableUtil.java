package com.trajan.negentropy.util;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
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

    public LocalDateTime getNextFutureTimeOf(LocalDateTime notBefore, LocalTime localTime) {
        if (localTime == null) return null;

        LocalDateTime result = localTime.atDate(notBefore.toLocalDate());
        return (result.isBefore(notBefore))
                ? result.plusDays(1)
                : result;
    }

    public LocalDateTime getNextFutureTimeOf(LocalDateTime notBefore, LocalDateTime localTime) {
        if (localTime == null) return null;
        return (localTime.isBefore(notBefore))
                ? localTime.plusDays(1)
                : localTime;
    }

    public LocalDateTime getTimeLimit(RoutineStepEntity step) {
        RoutineStepEntity parent = step.parentStep();

        LocalDateTime timeLimitByETA = null;

        if (parent != null && parent.link().isPresent()) {
            TaskLink parentLink = parent.link().get();
            if (parentLink.projectEtaLimit().isPresent()) {
                timeLimitByETA = getNextFutureTimeOf(
                        step.routine().startTime(),
                        parentLink.projectEtaLimit().get());
            }
        }

        return timeLimitByETA;
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
                case SUSPENDED, SKIPPED:
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
        return switch (timeable.status()) {
            case NOT_STARTED:
                yield timeable.duration();
            case ACTIVE, SUSPENDED, SKIPPED:
                yield timeable.duration().minus(getElapsedActiveDuration(timeable, time));
            case COMPLETED, EXCLUDED, POSTPONED, LIMIT_EXCEEDED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingDurationIncludingLimitExceeded(Timeable timeable, LocalDateTime time) {
        return switch (timeable.status()) {
            case NOT_STARTED, LIMIT_EXCEEDED:
                yield timeable.duration();
            case ACTIVE, SUSPENDED, SKIPPED:
                yield timeable.duration().minus(getElapsedActiveDuration(timeable, time));
            case COMPLETED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingNestedDuration(RoutineStepData<?> ancestor, LocalDateTime time) {
        return this.getRemainingNestedDuration(ancestor, time, false);
    }

    public Duration getRemainingNestedDuration(RoutineStepData<?> step, LocalDateTime time, boolean allowNegative) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED, SKIPPED:
                yield iterateToGetNestedDuration(step, time, allowNegative);
            case COMPLETED, EXCLUDED, POSTPONED, LIMIT_EXCEEDED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingNestedDurationIncludingLimitExceeded(RoutineStepData<?> step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED, SKIPPED, LIMIT_EXCEEDED:
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
        return LocalDateTime.now();
    }
}
