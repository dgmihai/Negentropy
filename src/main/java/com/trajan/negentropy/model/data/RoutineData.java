package com.trajan.negentropy.model.data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface RoutineData extends Data {
    List<? extends RoutineStepData> steps();
    RoutineData estimatedDuration(Duration duration);
    RoutineData estimatedDurationLastUpdatedTime(LocalDateTime time);
    LocalDateTime finishTime();

    default String typeName() {
        return "Routine";
    }
}
