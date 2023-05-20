package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.entity.RoutineEntity;
import com.trajan.negentropy.server.facade.model.Routine;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RoutineResponse extends Response {
    private final Routine routine;

    public RoutineResponse(boolean success, RoutineEntity routineEntity, String message) {
        super(success, message);
        this.routine = routineEntity == null ?
                null :
                DataContext.toDTO(routineEntity);
    }
}