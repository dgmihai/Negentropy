package com.trajan.negentropy.client;

import com.trajan.negentropy.server.entity.TaskNode;

public record TaskEntry(
        TaskNode node,
        TaskEntry parent
) { }