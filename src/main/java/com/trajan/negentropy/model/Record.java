package com.trajan.negentropy.model;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
public class Record {
    private TaskID taskId;
    private String name;
    private StepID stepId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration inactiveTime;
    private Duration elapsedTime = Duration.ZERO;
    private TimeableStatus result;

    public Record(RoutineStepEntity step) {
        this.taskId = ID.of(step.task());
        this.stepId = ID.of(step);
        this.name = step.name();
        this.startTime = step.startTime();
        this.endTime = step.finishTime();
        this.inactiveTime = step.elapsedSuspendedDuration();
        this.result = step.status();

        if (startTime != null && endTime != null) {
            this.elapsedTime = Duration.between(startTime, endTime).minus(inactiveTime);
        }
    }
}
