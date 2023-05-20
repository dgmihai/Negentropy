package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.entity.RoutineStepEntity;
import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RoutineStepResponse extends Response {
    private final RoutineStep oldStep;
    private final RoutineStep newStep;
    private final RoutineStatus routineStatus;

    public RoutineStepResponse(boolean success, String message,
                               RoutineStepEntity oldStep,
                               RoutineStepEntity newStep,
                               RoutineStatus routineStatus) {
        super(success, message);
        this.oldStep = DataContext.toDTO(oldStep);
        this.newStep = DataContext.toDTO(newStep);
        this.routineStatus = routineStatus;
    }
}