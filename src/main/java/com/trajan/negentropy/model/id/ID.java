package com.trajan.negentropy.model.id;

import com.fasterxml.jackson.annotation.JsonValue;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public abstract class ID implements Serializable {

    protected final long val;

    @EqualsAndHashCode(callSuper = true)
    public static abstract class TaskOrLinkID extends ID {
        public TaskOrLinkID(long val) {
            super(val);
        }
    };

    public static TaskID of(TaskEntity taskEntity) {
        return taskEntity == null ?
                null :
                new TaskID(taskEntity.id());
    }

    public static LinkID of(TaskLink taskLink) {
        return taskLink == null ?
                null :
                new LinkID(taskLink.id());
    }

    public static TagID of(TagEntity tagEntity) {
        return tagEntity == null ?
                null :
                new TagID(tagEntity.id());
    }

    public static RoutineID of(RoutineEntity routineEntity) {
        return routineEntity == null ?
                null :
                new RoutineID(routineEntity.id());
    }

    public static StepID of(RoutineStepEntity routineStepEntity) {
        return routineStepEntity == null ?
                null :
                new StepID(routineStepEntity.id());
    }

    public static class SyncID extends ID {
        public SyncID(long val) {
            super(val);
        }
    }

    public static class ChangeID extends ID {
        public ChangeID(int val) {
            super(val);
        }

        public ChangeID(long val) {
            super(val);
        }
    }

    public static class StepID extends ID {
        public StepID(long val) {
            super(val);
        }

        public static StepID nil() {
            return new StepID(-1);
        }
    }

    @Override
    @JsonValue
    public String toString() {
        return this.getClass().getSimpleName() + "(" + val +")";
    }
}