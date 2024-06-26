package com.trajan.negentropy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.HasTaskData.TaskData;
import com.trajan.negentropy.model.id.TaskID;
import lombok.*;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Set;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter(onMethod_={@JsonProperty})
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Task implements TaskData<Task>, PersistedDataDO<TaskID> {
    @EqualsAndHashCode.Include
    protected final TaskID id;
    protected String name;
    protected String description;
    protected Duration duration;
    protected Boolean required;
    protected Boolean project;
    protected Boolean difficult;
    protected Integer effort;
    protected Boolean starred;
    protected Boolean pinned;
    protected Boolean cleanup;
    @Nullable
    protected Set<Tag> tags;

    public Task() {
        id = null;
    }

    public Task copyWithoutID() {
        return new Task(null, name, description, duration, required, project, difficult, effort, starred, pinned, cleanup, tags);
    }

    public Task copyWithID(TaskID id) {
        return new Task(id, name, description, duration, required, project, difficult, effort, starred, pinned, cleanup, tags);
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
        if (difficult != null) result.append(", difficult=").append(difficult);
        if (effort != null) result.append(", effort=").append(effort);
        if (starred != null) result.append(", starred=").append(starred);
        if (pinned != null) result.append(", pinned=").append(pinned);
        if (cleanup != null) result.append(", cleanup=").append(cleanup);
        if (tags != null) result.append(", tags=").append(tags);

        result.append(")");
        return result.toString();
    }
}