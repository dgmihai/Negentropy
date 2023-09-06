package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.HasDuration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface RoutineStepData extends Data, HasDuration {
    List<? extends RoutineStepData> children();
    TimeableStatus status();
    LocalDateTime startTime();
    LocalDateTime finishTime();
    LocalDateTime lastSuspendedTime();
    Duration elapsedSuspendedDuration();

    default String typeName() {
        return "Routine Step";
    }
}
