package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.model.interfaces.HasDuration;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.util.TimeableUtil.TimeableAncestor;

import java.time.Duration;
import java.time.LocalDateTime;

public interface RoutineStepData <T extends RoutineStepData<T>> extends Ancestor<T>, Data, HasDuration, Timeable<T>, TimeableAncestor<T> {
    TimeableStatus status();
    LocalDateTime lastSuspendedTime();
    Duration elapsedSuspendedDuration();
    LocalDateTime startTime();
    LocalDateTime finishTime();
    String name();

    default String typeName() {
        return "Routine Step";
    }
}
