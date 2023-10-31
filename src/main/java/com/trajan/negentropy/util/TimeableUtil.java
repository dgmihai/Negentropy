package com.trajan.negentropy.util;

import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class TimeableUtil {

    public static TimeableUtil get() {
        return new TimeableUtil();
    }

    public Duration getElapsedActiveDuration(Timeable timeable, LocalDateTime time) {
        Duration elapsedSuspendedDuration = timeable.elapsedSuspendedDuration() != null
                ? timeable.elapsedSuspendedDuration()
                : Duration.ZERO;
        return switch (timeable.status()) {
            case NOT_STARTED:
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
    }

    public Duration getRemainingDuration(Timeable timeable, LocalDateTime time) {
        return switch (timeable.status()) {
            case NOT_STARTED:
                yield timeable.duration();
            case ACTIVE, SUSPENDED, SKIPPED:
                yield timeable.duration().minus(getElapsedActiveDuration(timeable, time));
            case COMPLETED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
        };
    }

    public Duration getRemainingNetDuration(RoutineStep step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED, SKIPPED:
                yield DFSUtil.traverse(step).stream()
                        .map(s -> {
                            Duration remaining = getRemainingDuration(s, time);
                            return remaining.isNegative() ? Duration.ZERO : remaining;
                        })
                        .reduce(Duration.ZERO, Duration::plus);
            case COMPLETED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
        };
    }

    public LocalDateTime currentTime() {
        return LocalDateTime.now();
    }
}
