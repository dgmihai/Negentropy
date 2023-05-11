package com.trajan.negentropy.client.tree.data;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Duration;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class TaskEntry {
    @ToString.Exclude
    private TaskEntry parent;
    private TaskNode node;
    private Task task;
    private Duration netDuration;
    private boolean hasChildren;
}