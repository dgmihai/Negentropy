package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A service implementation of the DataContext interface, managing persistence of entities.
 */
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
                taskEntity.tags().stream()
                        .map(DataContext::toDTO)
                        .collect(Collectors.toSet()),
                taskEntity.recurring());
    }

    static TaskNode toDTO(TaskLink link) {
        TaskID parentId = ID.of(link.parent());
        if (parentId != null) {
            List<TaskLink> siblings = link.parent().childLinks();
        }

        return new TaskNode(
                ID.of(link),
                parentId,
                ID.of(link.child()),
                link.position(),
                link.importance());
    }

    static Tag toDTO(TagEntity tagEntity) {
        return new Tag(
                ID.of(tagEntity),
                tagEntity.name());
    }

    TaskLink TESTONLY_mergeLink(TaskLink link);
    TaskEntity TESTONLY_mergeTask(TaskEntity taskEntity);
}