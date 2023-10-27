package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.HasDuration;
import com.trajan.negentropy.model.interfaces.TimeableAncestor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface RoutineStepData <T extends RoutineStepData<T>>  extends TimeableAncestor<T>, Data, HasDuration {
    List<T> children();
    TimeableStatus status();
    LocalDateTime lastSuspendedTime();
    Duration elapsedSuspendedDuration();

    default String typeName() {
        return "Routine Step";
    }
}
