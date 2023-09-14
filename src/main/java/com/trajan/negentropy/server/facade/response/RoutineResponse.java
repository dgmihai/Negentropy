package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.model.entity.routine.Routine;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RoutineResponse extends Response {
    private final Routine routine;

    public RoutineResponse(boolean success, Routine routine, String message) {
        super(success, message);
        this.routine = routine;
    }
}