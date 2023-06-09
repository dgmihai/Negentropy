package com.trajan.negentropy.util;

import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.interfaces.RoutineData;
import com.trajan.negentropy.server.facade.model.interfaces.RoutineStepData;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class RoutineUtil {

    public static Duration getStepElapsedActiveDuration(RoutineStepData step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED:
                yield Duration.ZERO;
            case ACTIVE:
                yield Duration.between(step.startTime(), time)
                        .minus(step.elapsedSuspendedDuration());
            case COMPLETED, SKIPPED:
                yield Duration.between(step.startTime(), step.finishTime())
                        .minus(step.elapsedSuspendedDuration());
            case SUSPENDED:
                yield Duration.between(step.startTime(), step.lastSuspendedTime())
                        .minus(step.elapsedSuspendedDuration());
        };
    }

    public static Duration getRemainingStepDuration(RoutineStepData step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED:
                yield step.task().duration();
            case ACTIVE, SUSPENDED:
                yield step.task().duration().minus(
                        getStepElapsedActiveDuration(step, time));
            case SKIPPED, COMPLETED:
                yield Duration.ZERO;
        };
    }

    public static LocalDateTime getRoutineStepETA(RoutineStepData step, LocalDateTime time) {
        return switch (step.status()) {
            case NOT_STARTED, ACTIVE, SUSPENDED:
                yield time.plus(getRemainingStepDuration(step, time));
            case SKIPPED, COMPLETED:
                yield time;
        };
    }

    public static Duration getRemainingRoutineDuration(RoutineData routine, LocalDateTime time) {
        return routine.steps().stream()
                .filter(step ->
                        !step.status().equals(TimeableStatus.SKIPPED) && !step.status().equals(TimeableStatus.COMPLETED))
                .map(step -> getRemainingStepDuration(step, time))
                .reduce(Duration.ZERO, Duration::plus);
    }

    public static void setRoutineDuration(RoutineData routine, LocalDateTime time) {
        routine.estimatedDuration(getRemainingRoutineDuration(routine, time));
        routine.estimatedDurationLastUpdatedTime(time);
    }
}
