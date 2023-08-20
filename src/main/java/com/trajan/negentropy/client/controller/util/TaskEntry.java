package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.interfaces.Descendant;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TaskEntry implements HasTaskNodeData, Descendant<TaskEntry> {
    @ToString.Exclude
    private TaskEntry parent;
    private TaskNode node;

    @Override
    public Task task() {
        return node.child();
    }
}