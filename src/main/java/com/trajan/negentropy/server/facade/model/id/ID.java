package com.trajan.negentropy.server.facade.model.id;

import com.trajan.negentropy.server.backend.entity.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ID {
    protected final long val;

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

    @Override
    public String toString() {
        return String.valueOf(val);
    }
}