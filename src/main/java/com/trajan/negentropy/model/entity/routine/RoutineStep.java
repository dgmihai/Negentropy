package com.trajan.negentropy.model.entity.routine;

import com.fasterxml.jackson.annotation.*;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.*;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RoutineStep.RoutineTaskStep.class, name = "RoutineTaskStep"),
        @JsonSubTypes.Type(value = RoutineStep.RoutineNodeStep.class, name = "RoutineNodeStep")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Getter(onMethod_={@JsonProperty})
@Setter
public abstract class RoutineStep implements Timeable<RoutineStep>, RoutineStepData<RoutineStep>, HasTaskData,
        MayHaveTaskNodeData {
    protected StepID id;

    protected RoutineID routineId;
    protected StepID parentId;

    protected List<RoutineStep> children;

    protected LocalDateTime startTime;
    protected LocalDateTime finishTime;
    protected LocalDateTime lastSuspendedTime;

    protected Duration elapsedSuspendedDuration;

    protected TimeableStatus status;

    @Override
    public String name() {
        return task().name();
    }

    @Override
    public String description() {
        return task().description();
    }

    @Override
    public Duration duration() {
        return task().duration();
    }

    abstract public Task task();
    abstract public RoutineStep task(Task task);

    abstract public TaskNode node();
    abstract public Optional<TaskNode> nodeOptional();
    abstract public RoutineStep node(TaskNode node);

    @Override
    public String toString() {
        return "RoutineStep(" + id + ")[name=" + name() + ", status=" + status + ", startTime=" + startTime
                + ", finishTime=" + finishTime + "]";
    }

    @Override
    public List<RoutineStep> descendants() {
        return DFSUtil.traverse(this);
    }

    @JsonTypeName("RoutineTaskStep")
    @Getter(onMethod_={@JsonProperty})
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoutineTaskStep extends RoutineStep {
        private Task task;

        @Override
        public RoutineTaskStep task(Task task) {
            this.task = task;
            return this;
        }

        @Override
        public TaskNode node() {
            return null;
        }

        @Override
        public Optional<TaskNode> nodeOptional() {
            return Optional.empty();
        }

        @Override
        public RoutineTaskStep node(TaskNode node) {
            this.task = node.task();
            return this;
        }
    }

    @JsonTypeName("RoutineNodeStep")
    @Getter(onMethod_={@JsonProperty})
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoutineNodeStep extends RoutineStep implements HasTaskNodeData {
        private TaskNode node;

        @Override
        public Task task() {
            return node.child();
        }

        @Override
        public RoutineNodeStep task(Task task) {
            this.node.child(task);
            return this;
        }

        @Override
        public Optional<TaskNode> nodeOptional() {
            return Optional.of(node);
        }

        public RoutineNodeStep node(TaskNode node) {
            this.node = node;
            return this;
        }
    }

    @Getter
    public static class  RoutineStepWrapper extends RoutineStep implements RoutineStepData<RoutineStep> {
        private RoutineData routine;

        @Override
        public Task task() {
            return null;
        }

        @Override
        public RoutineStep task(Task task) {
            return null;
        }

        @Override
        public TaskNode node() {
            return null;
        }

        @Override
        public Optional<TaskNode> nodeOptional() {
            return Optional.empty();
        }

        @Override
        public RoutineStep node(TaskNode node) {
            return null;
        }
    }
}
