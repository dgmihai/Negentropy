package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.Task.TaskDTO;
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
    TaskEntity mergeTask(Task task);
    TaskEntity mergeTaskTemplate(TaskID id, TaskDTO template);

    TaskLink mergeNode(TaskNode node);
    TaskLink mergeNode(TaskNodeDTO node);
    TaskLink mergeNodeTemplate(LinkID linkId, TaskNodeTemplateData<?> nodeTemplate);

    TagEntity mergeTag(TagEntity tagEntity);
    TagEntity mergeTag(Tag tag);

    TenetEntity mergeTenet(Tenet tenet);

    MoodEntity mergeMood(Mood mood);

    void deleteLink(TaskLink link);
    void deleteTenet(Long id);
    void deleteMood(Long id);

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

    Task toDO(TaskEntity taskEntity);

    TaskDTO toDTO(TaskEntity taskEntity);

    TaskNode toDO(TaskLink link);

    Routine toDO(RoutineEntity routineEntity);

    RoutineStep toDO(RoutineStepEntity routineStepEntity);

    Tenet toDO(TenetEntity tenetEntity);

    Mood toDO(MoodEntity moodEntity);
}