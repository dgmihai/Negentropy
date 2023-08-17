package com.trajan.negentropy.model;

import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.util.RoutineUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class RoutineStep implements RoutineStepData, Timeable, HasTaskNodeData, Ancestor<RoutineStep> {
    private StepID id;

    private TaskNode node;
    private Task taskRecord;

    private RoutineID routineId;
    private StepID parentId;

    private List<RoutineStep> children;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime lastSuspendedTime;

    private Duration elapsedSuspendedDuration;

    private TimeableStatus status;

    @Override
    public String name() {
        return task().name();
    }

    @Override
    public String description() {
        return task().description();
    }

    @Override
    public Duration remainingDuration(LocalDateTime time) {
        return RoutineUtil.getRemainingStepDuration(this, time);
    }

    @Override
    public Duration duration() {
        return task().duration();
    }

    @Override
    public Task task() {
        return node != null
                ? node.task()
                : taskRecord;
    }
}
