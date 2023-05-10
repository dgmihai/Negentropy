package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Builder
@Getter
@Setter
@ToString
public class TaskNodeDTO {
    private TaskID parentId;
    private TaskID childId;
    private Integer position;
    private Integer importance;

    public TaskNodeDTO(TaskNode node) {
        this.parentId = node.parentId();
        this.childId = node.childId();
        this.position = node.position();
        this.importance = node.importance();
    }

    public TaskNodeDTO(TaskLink link) {
        this.parentId = ID.of(link.parent());
        this.childId = ID.of(link.child());
        this.position = link.position();
        this.importance = link.importance();
    }
}
