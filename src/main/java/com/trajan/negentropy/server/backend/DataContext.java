package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.*;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;

import java.time.LocalDateTime;

/**
 * A service implementation of the DataContext interface, managing persistence of entities.
 */
public interface DataContext {
    TaskEntity merge(Task task);
    TaskEntity merge(TaskID id, Task template);

    TaskLink merge(TaskNode node);

    TaskLink merge(TaskNode node, boolean createTrackingStep);

    TaskLink merge(TaskNodeDTO node);
    TaskLink merge(LinkID linkId, TaskNodeTemplateData<?> nodeTemplate);

    TagEntity merge(TagEntity tagEntity);
    TagEntity merge(Tag tag);

    TenetEntity merge(Tenet tenet);
    MoodEntity merge(Mood mood);
    StressorEntity merge(Stressor stressor);

    void deleteLink(TaskLink link);
    void deleteTenet(Long id);
    void deleteMood(Long id);
    void deleteStressor(Long id);

    TaskLink TESTONLY_mergeLink(TaskLink link);

    TaskEntity TESTONLY_mergeTask(TaskEntity taskEntity);

    RoutineEntity TESTONLY_mergeRoutine(RoutineEntity children);

    RoutineStepEntity TESTONLY_mergeRoutineStep(RoutineStepEntity one);

    void deleteAllOrphanedTags();

    @VisibleForTesting
    static LocalDateTime now() {
        return LocalDateTime.now();
    };

    Tag toDO(TagEntity tagEntity);

    Task toLazyDO(TaskEntity taskEntity);
    Task toEagerDO(TaskEntity taskEntity);

    TaskNode toLazyDO(TaskLink link);
    TaskNode toEagerDO(TaskLink link);

    Routine toDO(RoutineEntity routineEntity);

    RoutineStep toDO(RoutineStepEntity routineStepEntity);

    Tenet toDO(TenetEntity tenetEntity);
    Mood toDO(MoodEntity moodEntity);
    Stressor toDO(StressorEntity stressorEntity);
}