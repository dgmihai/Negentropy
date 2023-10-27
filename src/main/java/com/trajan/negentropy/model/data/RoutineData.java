package com.trajan.negentropy.model.data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface RoutineData <T extends RoutineStepData<T>> extends Data {
    Boolean autoSync();
    List<T> children();
    T rootStep();
    List<T> getAllChildren();
    RoutineData<T> estimatedDuration(Duration duration);
    RoutineData<T> estimatedDurationLastUpdatedTime(LocalDateTime time);
    LocalDateTime finishTime();

    default String typeName() {
        return "Routine";
    }
}
