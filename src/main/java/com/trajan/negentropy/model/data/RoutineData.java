package com.trajan.negentropy.model.data;

import java.time.LocalDateTime;
import java.util.List;

public interface RoutineData <T extends RoutineStepData<T>> extends Data {
    Boolean autoSync();
    List<T> children();
    T rootStep();
    List<T> getAllChildren();
    LocalDateTime startTime();
    LocalDateTime finishTime();

    default String typeName() {
        return "Routine";
    }
}
