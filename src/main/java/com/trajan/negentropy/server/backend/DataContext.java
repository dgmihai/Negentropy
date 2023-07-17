package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.server.backend.entity.*;
import com.trajan.negentropy.server.facade.model.*;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * A service implementation of the DataContext interface, managing persistence of entities.
 */
@Transactional
public interface DataContext {
    void addToTimeEstimateOfAllAncestors(Duration change, TaskID descendantId);
    void addToTimeEstimateOfTask(Duration change, TaskID taskId);

    TaskEntity mergeTask(Task task);

    TaskLink mergeNode(TaskNode node);
    TaskLink mergeNode(TaskNodeDTO node);

    TagEntity mergeTag(TagEntity tagEntity);
    TagEntity mergeTag(Tag tag);

    void deleteLink(TaskLink link);

    static Task toDTO(TaskEntity taskEntity) {
        return new Task(ID.of(taskEntity),
                taskEntity.name(),
                taskEntity.description(),
                taskEntity.duration(),
                taskEntity.block(),
                taskEntity.project(),
                taskEntity.tags().stream()
                        .map(DataContext::toDTO)
                        .collect(Collectors.toSet()),
                !taskEntity.childLinks().isEmpty());
    }

    static TaskNode toDTO(TaskLink link) {
        return new TaskNode(
                ID.of(link),
                ID.of(link.parent()),
                toDTO(link.child()),
                link.position(),
                link.importance(),
                link.createdAt(),
                link.completed(),
                link.recurring(),
                link.cron(),
                link.scheduledFor(),
                link.projectDuration());
    }

    static Tag toDTO(TagEntity tagEntity) {
        return new Tag(
                ID.of(tagEntity),
                tagEntity.name());
    }

    static Routine toDTO(RoutineEntity routineEntity) {
        return new Routine(
                ID.of(routineEntity),
                routineEntity.steps().stream()
                        .map(DataContext::toDTO)
                        .toList(),
                routineEntity.currentPosition(),
                routineEntity.estimatedDuration(),
                routineEntity.estimatedDurationLastUpdatedTime(),
                routineEntity.status());
    }

    static RoutineStep toDTO(RoutineStepEntity routineStepEntity) {
        return new RoutineStep(
                ID.of(routineStepEntity),
                toDTO(routineStepEntity.link()),
                ID.of(routineStepEntity.routine()),
                ID.of(routineStepEntity.parent()),
                routineStepEntity.children().stream()
                        .map(DataContext::toDTO)
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