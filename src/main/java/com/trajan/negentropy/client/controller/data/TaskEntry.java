package com.trajan.negentropy.client.controller.data;

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
    private Duration netDuration;

    public Task task() {
        return node.child();
    }
}