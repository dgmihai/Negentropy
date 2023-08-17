package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.HasDuration;

import java.time.Duration;
import java.time.LocalDateTime;

public interface RoutineStepData extends Data, HasDuration {
    TimeableStatus status();
    LocalDateTime startTime();
    LocalDateTime finishTime();
    LocalDateTime lastSuspendedTime();
    Duration elapsedSuspendedDuration();
}
