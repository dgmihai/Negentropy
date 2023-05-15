package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import lombok.*;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(callSuper = true)
public class TaskNode extends TaskNodeDTO {
    @EqualsAndHashCode.Include
    @NonNull
    private LinkID linkId;

    public TaskNode(@NonNull LinkID linkId, TaskID parentId, TaskID childId, int position, int importance) {
        super(parentId, childId, position, importance);
        this.linkId = linkId;
    }
}