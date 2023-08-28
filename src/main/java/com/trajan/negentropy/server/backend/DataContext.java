package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.data.HasTaskData.TaskTemplateData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TenetEntity;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A service implementation of the DataContext interface, managing persistence of entities.
 */
public interface DataContext {
    void addToNetDurationOfAllAncestors(Duration change, TaskID descendantId);
    void addToNetDurationOfTask(Duration change, TaskID taskId);

    TaskEntity mergeTask(Task task);
    TaskEntity mergeTaskTemplate(TaskID id, TaskTemplateData<Task, Tag> template);

    TaskLink mergeNode(TaskNode node);
    TaskLink mergeNode(TaskNodeDTO node);
    TaskLink mergeNodeTemplate(LinkID linkId, TaskNodeTemplateData<?> nodeTemplate);

    TagEntity mergeTag(TagEntity tagEntity);
    TagEntity mergeTag(Tag tag);

    TenetEntity mergeTenet(Tenet tenet);

    void deleteLink(TaskLink link);
    void deleteTenet(Long id);

    TaskLink TESTONLY_mergeLink(TaskLink link);

    void deleteAllOrphanedTags();

    TaskEntity TESTONLY_mergeTask(TaskEntity taskEntity);

    @VisibleForTesting
    static LocalDateTime now() {
        return LocalDateTime.now();
    };

    Tag toDO(TagEntity tagEntity);

    Task toDO(TaskEntity taskEntity);

    TaskNode toDO(TaskLink link);

    Routine toDO(RoutineEntity routineEntity);

    RoutineStep toDO(RoutineStepEntity routineStepEntity);

    Tenet toDO(TenetEntity tenetEntity);
}