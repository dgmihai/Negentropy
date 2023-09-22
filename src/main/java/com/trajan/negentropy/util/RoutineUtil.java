package com.trajan.negentropy.util;

import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.data.RoutineStepData;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class RoutineUtil {

    public static Duration getStepElapsedActiveDuration(RoutineStepData step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
            case ACTIVE:
                yield step.startTime() != null
                        ? Duration.between(step.startTime(), time)
                            .minus(step.elapsedSuspendedDuration())
                        : Duration.ZERO;
            case COMPLETED, SKIPPED:
                yield step.startTime() != null
                        ? Duration.between(step.startTime(), step.finishTime())
                            .minus(step.elapsedSuspendedDuration())
                        : Duration.ZERO;
            case SUSPENDED:
                yield step.startTime() != null
                        ? Duration.between(step.startTime(), step.lastSuspendedTime())
                            .minus(step.elapsedSuspendedDuration())
                        : Duration.ZERO;
        };
    }

    public static Duration getRemainingStepDuration(RoutineStepData step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED:
                yield step.duration();
            case ACTIVE, SUSPENDED:
                yield step.duration().minus(
                        getStepElapsedActiveDuration(step, time));
            case SKIPPED, COMPLETED, EXCLUDED, POSTPONED:
                yield Duration.ZERO;
        };
    }

    public static LocalDateTime getRoutineStepETA(RoutineStepData step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED:
                yield time.plus(getRemainingStepDuration(step, time));
            case SKIPPED, COMPLETED, POSTPONED, EXCLUDED:
                yield time;
        };
    }

    public static Duration getRemainingRoutineDuration(RoutineData routine, LocalDateTime time) {
        try {
            return routine.getAllChildren().stream()
                    .map(step -> getRemainingStepDuration(step, time))
                    .reduce(Duration.ZERO, Duration::plus);
        } catch (Throwable e) {
            log.error("Error calculating remaining routine duration.", e);
            throw e;
        }
    }

    public static void setRoutineDuration(RoutineData routine, LocalDateTime time) {
        routine.estimatedDuration(getRemainingRoutineDuration(routine, time));
        routine.estimatedDurationLastUpdatedTime(time);
    }
}
