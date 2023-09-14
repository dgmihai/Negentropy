package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.TimeableStatus;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RoutineStepResponse extends Response {
    private final RoutineStep oldStep;
    private final RoutineStep newStep;
    private final TimeableStatus routineStatus;

    public RoutineStepResponse(boolean success, String message,
                               RoutineStep oldStep,
                               RoutineStep newStep,
                               TimeableStatus routineStatus) {
        super(success, message);
        this.oldStep = oldStep;
        this.newStep = newStep;
        this.routineStatus = routineStatus;
    }
}