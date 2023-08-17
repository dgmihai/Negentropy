package com.trajan.negentropy.model;

import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.HasTaskData.TaskData;
import com.trajan.negentropy.model.id.TaskID;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Task implements TaskData<Task, Tag>, PersistedDataDO<TaskID> {
    @EqualsAndHashCode.Include
    private final TaskID id;
    private String name;
    private String description;
    private Duration duration;
    private Boolean required;
    private Boolean project;
    private Set<Tag> tags;

    @Setter
    private Boolean hasChildren;

    public Task() {
        id = null;
    }

    @Override
    public Set<Tag> tags() {
        return tags != null
                ? Collections.unmodifiableSet(tags)
                : null;
    }

    public Task copyWithoutID() {
        Task copy = new Task();
        copy.name = this.name;
        copy.description = this.description;
        copy.duration = this.duration;
        copy.required = this.required;
        copy.project = this.project;
        if (this.tags != null) {
            copy.tags = new HashSet<>(this.tags);
        }
        copy.hasChildren = this.hasChildren;

        return copy;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Task(");
        result.append("id=").append(id);

        if (name != null) result.append(", name=\"").append(name).append("\"");
        if (description != null) result.append(", description=\"").append(description).append("\"");
        if (duration != null) result.append(", duration=").append(duration);
        if (required != null) result.append(", required=").append(required);
        if (project != null) result.append(", project=").append(project);
        if (tags != null) result.append(", tags=").append(tags);

        result.append(")");
        return result.toString();
    }
}