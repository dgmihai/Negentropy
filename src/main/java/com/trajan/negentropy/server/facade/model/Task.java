package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.facade.model.id.TaskID;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.Set;

@RequiredArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Task {
    @EqualsAndHashCode.Include
    private final TaskID id;
    private String name;
    private String description;
    private Duration duration;
    private Set<Tag> tags;
    private boolean oneTime;

    public Task() {
        id = null;
    }

    @Override
    public String toString() {
        return "Task[" + id + ", name: " + name + "]";
    }
}