package com.trajan.negentropy.client.controller.data;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TaskEntry {
    @ToString.Exclude
    private TaskEntry parent;
    private TaskNode node;
    @EqualsAndHashCode.Exclude
    private Duration netDuration;

    public Task task() {
        return node.child();
    }
}