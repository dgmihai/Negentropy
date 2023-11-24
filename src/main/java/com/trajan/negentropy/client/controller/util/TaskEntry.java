package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskNodeData.HasTaggedTaskNodeData;
import com.trajan.negentropy.model.interfaces.Descendant;
import lombok.*;

import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TaskEntry implements HasTaggedTaskNodeData, Descendant<TaskEntry> {
    @ToString.Exclude
    private TaskEntry parent;
    private TaskNode node;
    private Set<Tag> tags;

    @Override
    public TaskDTO task() {
        return new TaskDTO(node.child(), tags);
    }
}