package com.trajan.negentropy.model.id;

import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Accessors(fluent = true)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public abstract class ID implements Serializable {
    protected final long val;

    public interface TaskOrLinkID {}

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

    @Override
    public String toString() {
        return "ID(" + val +")";
    }
}