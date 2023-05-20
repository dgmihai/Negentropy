package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.interfaces.RoutineData;
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
public class Routine implements RoutineData {
    private RoutineID id;

    private List<RoutineStep> steps;

    private int currentPosition;

    private Duration estimatedDuration;
    private LocalDateTime estimatedDurationLastUpdatedTime;

    private RoutineStatus status;

    public RoutineStep currentStep() {
        return steps.get(currentPosition);
    }
}
