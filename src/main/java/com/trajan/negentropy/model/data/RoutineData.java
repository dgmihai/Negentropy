package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TimeableStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RoutineData <T extends RoutineStepData<T>> extends Data {
    Boolean autoSync();
    List<T> children();
    List<T> getDescendants();
    Integer currentPosition();

    default T currentStep() {
        return getDescendants().get(currentPosition());
    }

    default LocalDateTime startTime() {
        return children().get(0).startTime();
    }

    default LocalDateTime finishTime() {
        return currentStep().finishTime();
    }

    default int countSteps() {
        return getDescendants().size();
    }

    default String typeName() {
        return "Routine";
    }

    default boolean hasExcludedSteps() {
        return getDescendants().stream()
                .anyMatch(step -> step.status().equals(TimeableStatus.EXCLUDED));
    }
}
