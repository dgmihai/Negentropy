package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.interfaces.RoutineData;
import com.trajan.negentropy.server.facade.model.interfaces.Timeable;
import com.trajan.negentropy.util.RoutineUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class Routine implements RoutineData, Timeable {
    private RoutineID id;

    @ToString.Exclude
    private List<RoutineStep> steps;

    private int currentPosition;

    private Duration estimatedDuration;
    private LocalDateTime estimatedDurationLastUpdatedTime;

    private TimeableStatus status;

    public RoutineStep currentStep() {
        return steps.get(currentPosition);
    }
    public RoutineStep rootStep() {
        return steps.get(0);
    }

    @Override
    @ToString.Include
    public String name() {
        return rootStep().name();
    }

    @Override
    public String description() {
        return rootStep().description();
    }

    @Override
    public Duration remainingDuration(LocalDateTime time) {
        return RoutineUtil.getRemainingRoutineDuration(this, time);
    }

    @Override
    public LocalDateTime finishTime() {
        return currentStep().finishTime();
    }
}
