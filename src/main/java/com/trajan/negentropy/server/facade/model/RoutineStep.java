package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.model.interfaces.RoutineStepData;
import com.trajan.negentropy.util.RoutineUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class RoutineStep implements RoutineStepData, Timeable {
    private StepID id;

    private Task task;

    private RoutineID routineId;
    private StepID parentId;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime lastSuspendedTime;

    private Duration elapsedSuspendedDuration;

    private TimeableStatus status;

    @Override
    public String name() {
        return task.name();
    }

    @Override
    public String description() {
        return task.description();
    }

    @Override
    public Duration remainingDuration(LocalDateTime time) {
        return RoutineUtil.getRemainingStepDuration(this, time);
    }
}
