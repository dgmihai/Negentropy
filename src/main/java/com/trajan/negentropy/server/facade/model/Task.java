package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Accessors(fluent = true)
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Task {
    @EqualsAndHashCode.Include
    private final TaskID id;
    private String name;
    private String description;
    private Duration duration;
    private Set<TagEntity> tags = new HashSet<>();

    @Override
    public String toString() {
        return "Task(" + id + ", name: " + name + ")";
    }
}