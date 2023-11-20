package com.trajan.negentropy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.HasTaskData.TaskData;
import com.trajan.negentropy.model.id.TaskID;
import lombok.*;

import java.time.Duration;
import java.util.Collection;
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

    public Task() {
        id = null;
    }

    public Task copyWithoutID() {
        return new Task(null, name, description, duration, required, project, difficult);
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

        result.append(")");
        return result.toString();
    }

    @Getter(onMethod_={@JsonProperty})
    @Setter
    @NoArgsConstructor
    public static class TaskDTO extends Task {
        private Set<Tag> tags;

        public TaskDTO(TaskID id) {
            super(id);
        }

        public TaskDTO(TaskID id, String name, String description, Duration duration, Boolean required, Boolean project, Boolean difficult, Set<Tag> tags) {
            super(id, name, description, duration, required, project, difficult);
            this.tags = tags;
        }

        public TaskDTO(Task task, Collection<Tag> tags) {
            super(task.id, task.name, task.description, task.duration, task.required, task.project, task.difficult);
            this.tags = Set.copyOf(tags);
        }

        @Override
        public TaskDTO copyWithoutID() {
            return new TaskDTO(null, name, description, duration, required, project, difficult, tags);
        }
    }
}