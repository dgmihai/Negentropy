package com.trajan.negentropy.server.facade.model.interfaces;

import com.trajan.negentropy.server.backend.entity.status.StepStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public interface RoutineStepData {
    StepStatus status();
    TaskData task();
    LocalDateTime startTime();
    LocalDateTime finishTime();
    LocalDateTime lastSuspendedTime();
    Duration elapsedSuspendedDuration();


}
