package com.trajan.negentropy.server.facade.model;

public record TaskLink(
        Long id,
        int priority,
        Integer position,
        Long parentId,
        Long childId)
{ }
