package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.model.RoutineStep;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.server.backend.DataContext;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RoutineStepResponse extends Response {
    private final RoutineStep oldStep;
    private final RoutineStep newStep;
    private final TimeableStatus routineStatus;

    public RoutineStepResponse(boolean success, String message,
                               RoutineStepEntity oldStep,
                               RoutineStepEntity newStep,
                               TimeableStatus routineStatus) {
        super(success, message);
        this.oldStep = DataContext.toDO(oldStep);
        this.newStep = DataContext.toDO(newStep);
        this.routineStatus = routineStatus;
    }
}