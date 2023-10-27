package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.HasTaskData;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.MayHaveTaskNodeData;
import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public abstract class RoutineStep implements RoutineStepData<RoutineStep>, HasTaskData, MayHaveTaskNodeData {
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
    @ToString.Include
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

    @Getter
    @AllArgsConstructor
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

    @Getter
    @AllArgsConstructor
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
}
