package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.facade.model.Task;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TaskResponse extends Response {
    private final Task task;

    public TaskResponse(Boolean success, TaskEntity task, String message) {
        super(success, message);
        this.task = task == null ?
                null :
                DataContext.toDTO(task);
    }
}