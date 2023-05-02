package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.facade.model.EntityMapper;
import com.trajan.negentropy.server.facade.model.Task;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TaskResponse extends Response {
    private final Task task;

    public TaskResponse(Boolean success, TaskEntity task, String message) {
        super(success, message);
        if (task != null) {
            this.task = EntityMapper.toDTO(task);
        } else {
            this.task = null;
        }
    }
}