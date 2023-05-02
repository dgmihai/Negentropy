package com.trajan.negentropy.server.facade.model;

public record TaskNode(
        LinkID linkId,
        int priority,
        Integer position,
        TaskID parentId,
        TaskID childId)
{ }