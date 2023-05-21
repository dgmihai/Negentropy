package com.trajan.negentropy.server.facade.model.interfaces;

import com.trajan.negentropy.server.backend.entity.TimeableStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public interface RoutineStepData {
    TimeableStatus status();
    TaskData task();
    LocalDateTime startTime();
    LocalDateTime finishTime();
    LocalDateTime lastSuspendedTime();
    Duration elapsedSuspendedDuration();
}
