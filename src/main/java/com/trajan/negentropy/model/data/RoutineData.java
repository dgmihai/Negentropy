package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.util.TimeableUtil.TimeableAncestor;

import java.time.LocalDateTime;
import java.util.List;

public interface RoutineData <T extends RoutineStepData<T>> extends Data, TimeableAncestor<T> {
    Boolean autoSync();
    List<T> children();
    List<T> descendants();
    Integer currentPosition();

    default T currentStep() {
        return descendants().get(currentPosition());
    }

    default LocalDateTime startTime() {
        return children().get(0).startTime();
    }

    default LocalDateTime finishTime() {
        return currentStep().finishTime();
    }

    default int countSteps() {
        return descendants().size();
    }

    default String typeName() {
        return "Routine";
    }

    default boolean hasExcludedSteps() {
        return descendants().stream()
                .anyMatch(step -> step.status().equals(TimeableStatus.EXCLUDED));
    }
}
