package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.data.HasTaskData.TaskTemplateData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * A service implementation of the DataContext interface, managing persistence of entities.
 */
public interface DataContext {
    void addToTimeEstimateOfAllAncestors(Duration change, TaskID descendantId);
    void addToTimeEstimateOfTask(Duration change, TaskID taskId);

    TaskEntity mergeTask(Task task);
    TaskEntity mergeTaskTemplate(TaskID id, TaskTemplateData<Task, Tag> template);

    TaskLink mergeNode(TaskNode node);
    TaskLink mergeNode(TaskNodeDTO node);
    TaskLink mergeNodeTemplate(LinkID linkId, TaskNodeTemplateData<?> nodeTemplate);

    TagEntity mergeTag(TagEntity tagEntity);
    TagEntity mergeTag(Tag tag);

    void deleteLink(TaskLink link);

    static Task toDO(TaskEntity taskEntity) {
        return new Task(ID.of(taskEntity),
                taskEntity.name(),
                taskEntity.description(),
                taskEntity.duration(),
                taskEntity.required(),
                taskEntity.project(),
                taskEntity.tags().stream()
                        .map(DataContext::toDO)
                        .collect(Collectors.toSet()),
                !taskEntity.childLinks().isEmpty());
    }

    static TaskNode toDO(TaskLink link) {
        return new TaskNode(
                ID.of(link),
                ID.of(link.parent()),
                toDO(link.child()),
                link.position(),
                link.importance(),
                link.createdAt(),
                link.completed(),
                link.recurring(),
                link.cron(),
                link.scheduledFor(),
                link.projectDuration());
    }

    static Tag toDO(TagEntity tagEntity) {
        return new Tag(
                ID.of(tagEntity),
                tagEntity.name());
    }

    static Routine toDO(RoutineEntity routineEntity) {
        return new Routine(
                ID.of(routineEntity),
                routineEntity.steps().stream()
                        .map(DataContext::toDO)
                        .toList(),
                routineEntity.currentPosition(),
                routineEntity.estimatedDuration(),
                routineEntity.estimatedDurationLastUpdatedTime(),
                routineEntity.status());
    }

    static RoutineStep toDO(RoutineStepEntity routineStepEntity) {
        return new RoutineStep(
                ID.of(routineStepEntity),
                routineStepEntity.link() != null
                        ? toDO(routineStepEntity.link())
                        : null,
                routineStepEntity.taskRecord() != null
                        ? toDO(routineStepEntity.taskRecord())
                        : null,
                ID.of(routineStepEntity.routine()),
                ID.of(routineStepEntity.parent()),
                routineStepEntity.children().stream()
                        .map(DataContext::toDO)
                        .toList(),
                routineStepEntity.startTime(),
                routineStepEntity.finishTime(),
                routineStepEntity.lastSuspendedTime(),
                routineStepEntity.elapsedSuspendedDuration(),
                routineStepEntity.status());
    }

    TaskLink TESTONLY_mergeLink(TaskLink link);

    void deleteAllOrphanedTags();

    TaskEntity TESTONLY_mergeTask(TaskEntity taskEntity);

    @VisibleForTesting
    static LocalDateTime now() {
        return LocalDateTime.now();
    };
}