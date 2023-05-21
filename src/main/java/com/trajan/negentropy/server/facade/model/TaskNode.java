package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString
public class TaskNode extends TaskNodeInfo {
    @EqualsAndHashCode.Include
    @NonNull
    private LinkID linkId;

    private TaskID parentId;
    @NotNull
    private Task child;
    private Integer position;
    private Integer importance;
    private Boolean recurring;
    private Boolean completed;
}