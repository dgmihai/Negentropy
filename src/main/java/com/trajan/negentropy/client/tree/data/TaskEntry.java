package com.trajan.negentropy.client.tree.data;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;

public record TaskEntry(
        TaskEntry parent,
        TaskNode node,
        Task task
) { }