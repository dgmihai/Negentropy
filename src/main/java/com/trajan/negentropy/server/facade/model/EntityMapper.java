package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.AbstractEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@Transactional
public class EntityMapper {

    /**
     * Creates a DTO model of a persisted Task entity.
     *
     * @param taskEntity The Task entity to create a DTO from.
     * @return The Task DTO object.
     */
    public static Task toDTO(TaskEntity taskEntity) {
        return new Task(new TaskID(taskEntity.id()))
                .name(taskEntity.name())
                .description(taskEntity.description())
                .duration(taskEntity.duration());
    }

    /**
     * Merge a detached or transient Task object to a persistence-managed entity.
     *
     * @param task The Task object to be persisted or merged.
     * @return The persisted Task entity.
     */
    public static TaskEntity merge(Task task, TaskEntity taskEntity) {
        return taskEntity
                .name(Objects.requireNonNullElse(
                        task.name(), taskEntity.name()))
                .duration(Objects.requireNonNullElse(
                        task.duration(), taskEntity.duration()))
                .description(Objects.requireNonNullElse(
                        task.description(), taskEntity.description()))
                .tags(taskEntity.tags())
                .childLinks(taskEntity.childLinks())
                .parentLinks(taskEntity.parentLinks());
    }

    /**
     * Creates a DTO node model of a persisted TaskLink entity.
     *
     * @param link The TaskLink entity to create a DTO from.
     * @return The TaskNode DTO object.
     */
    public static TaskNode toDTO(TaskLink link) {
        return new TaskNode(
                new LinkID(link.id()),
                link.priority(),
                link.position(),
                idOrNull(link.parent()),
                idOrNull(link.child()));
    }

    /**
     * Merge a TaskNode object to a persistence-managed TaskLink entity.
     *
     * @param node The TaskNode object to be persisted or merged.
     * @return The persisted TaskLink entity.
     */
    public static TaskLink merge(TaskNode node, TaskLink link) {
        return link
                .priority(node.priority());
    }

    private static TaskID idOrNull(AbstractEntity entity) {
        return entity == null ? null : new TaskID(entity.id());
    }
}
