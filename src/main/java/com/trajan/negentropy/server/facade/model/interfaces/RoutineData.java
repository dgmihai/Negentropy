package com.trajan.negentropy.server.facade.model.interfaces;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface RoutineData {
    List<? extends RoutineStepData> steps();
    RoutineData estimatedDuration(Duration duration);
    RoutineData estimatedDurationLastUpdatedTime(LocalDateTime time);
    LocalDateTime finishTime();
}
