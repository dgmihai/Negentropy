package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.facade.model.EntityMapper;
import com.trajan.negentropy.server.facade.model.Task;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TaskResponse {
    private final Boolean success;
    private final Task task;
    private final String message;

    public TaskResponse(Boolean success, TaskEntity task, String message) {
        this.success = success;
        this.task = task != null ? EntityMapper.toDTO(task) : null;
        this.message = message;
    }
}