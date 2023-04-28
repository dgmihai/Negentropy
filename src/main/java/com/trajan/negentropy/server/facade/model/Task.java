package com.trajan.negentropy.server.facade.model;

import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.List;

@Builder(toBuilder = true)
@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Task {
    @Builder.Default
    @EqualsAndHashCode.Include
    private final Long id = null;
    private String name;
    private String description;
    private Duration duration;

    @Builder.Default
    private final List<TaskLink> parentLinks = null;
    @Builder.Default
    private final List<TaskLink> childLinks = null;

    @Override
    public String toString() {
        return "Task(" + super.toString() + ", name: " + name + ")";
    }
}