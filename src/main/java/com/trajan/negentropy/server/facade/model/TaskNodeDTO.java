package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class TaskNodeDTO extends TaskNodeInfo {
    private TaskID parentId;
    private TaskID childId;

    public TaskNodeDTO(TaskID parentId, TaskID childId, Integer position, Integer importance, Boolean recurring, Boolean completed) {
        this.parentId = parentId;
        this.childId = childId;
        this.position = position;
        this.importance = importance;
        this.recurring = recurring;
        this.completed = completed;
    }
    
    public TaskNodeDTO(TaskNode node) {
        this.parentId = node.parentId();
        this.childId = node.child().id();
        this.position = node.position();
        this.importance = node.importance();
        this.recurring = node.recurring();
        this.completed = node.completed();
    }

    public TaskNodeDTO(TaskLink link) {
        this.parentId = ID.of(link.parent());
        this.childId = ID.of(link.child());
        this.position = link.position();
        this.importance = link.importance();
        this.recurring = link.recurring();
        this.completed = link.completed();
    }

    public TaskNodeDTO(TaskID parentId, TaskID childId, TaskNodeInfo info) {
        this.parentId = parentId;
        this.childId = childId;
        if (info != null) {
            this.position = info.position();
            this.importance = info.importance();
            this.recurring = info.recurring();
            this.completed = info.completed();
        }
    }
}
