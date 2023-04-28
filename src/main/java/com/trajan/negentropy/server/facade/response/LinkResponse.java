package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.facade.model.EntityMapper;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskLink;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class LinkResponse {
    private final Boolean success;
    private final Task parent;
    private final Task child;
    private final TaskLink link;
    private final String message;

    public LinkResponse(Boolean success, TaskEntity parent, TaskEntity child, TaskLinkEntity link, String message) {
        this.success = success;
        this.parent = parent != null ? EntityMapper.toDTO(parent) : null;
        this.child = child != null ? EntityMapper.toDTO(child) : null;
        this.link = link != null ? EntityMapper.toDTO(link) : null;
        this.message = message;
    }
}
