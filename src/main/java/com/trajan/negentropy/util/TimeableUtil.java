package com.trajan.negentropy.util;

import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.model.interfaces.TimeableAncestor;
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
        Duration elapsedSuspendedDuration = (timeable instanceof RoutineStepData<?> stepData)
                ? stepData.elapsedSuspendedDuration()
                : Duration.ZERO;
        return switch (timeable.status()) {
            case NOT_STARTED:
                yield Duration.ZERO;
            case ACTIVE:
                yield timeable.startTime() != null
                        ? Duration.between(timeable.startTime(), time)
                            .minus(elapsedSuspendedDuration)
                        : Duration.between(timeable.startTime(), time);
            case COMPLETED, SKIPPED:
                yield timeable.startTime() != null
                        ? Duration.between(timeable.startTime(), timeable.finishTime())
                            .minus(elapsedSuspendedDuration)
                        : Duration.ZERO;
            case SUSPENDED, EXCLUDED, POSTPONED:
                if (timeable.startTime() != null) {
                        if (timeable instanceof RoutineStepData<?> stepData) {
                            yield Duration.between(timeable.startTime(), stepData.lastSuspendedTime())
                                    .minus(elapsedSuspendedDuration);
                        } else if (timeable.finishTime() != null) {
                            yield Duration.between(timeable.startTime(), timeable.finishTime())
                                    .minus(elapsedSuspendedDuration);
                        } else {
                            yield Duration.between(timeable.startTime(), time)
                                    .minus(elapsedSuspendedDuration);
                        }
                } else {
                    yield Duration.ZERO;
                }
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

    public <T extends TimeableAncestor<T>> Duration getRemainingNetDuration(T step, LocalDateTime time) {
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

//    public static LocalDateTime getETA(TimeableAncestor<?> step, LocalDateTime time) {
//        return switch (step.status()) {
//            case NOT_STARTED, ACTIVE, SUSPENDED:
//                yield time.plus(getRemainingNetDuration(step, time));
//            case SKIPPED, COMPLETED, POSTPONED, EXCLUDED:
//                yield time;
//        };
//    }

    public <T extends RoutineStepData<T>> Duration getRemainingRoutineDuration(RoutineData<T> routine, LocalDateTime time) {
        try {
            return getRemainingNetDuration(routine.rootStep(), time);
        } catch (Throwable e) {
            log.error("Error calculating remaining routine duration.", e);
            throw e;
        }
    }

    public void setRoutineDuration(RoutineData<?> routine, LocalDateTime time) {
        routine.estimatedDuration(getRemainingRoutineDuration(routine, time));
    }

    public LocalDateTime currentTime() {
        return LocalDateTime.now();
    }
}
