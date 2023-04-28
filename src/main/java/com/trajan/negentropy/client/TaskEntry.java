package com.trajan.negentropy.client;

import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;

public record TaskEntry(
        TaskLinkEntity node,
        TaskEntry parent
) { }