package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.backend.entity.AbstractEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EntityMapper {

    @Autowired private DataContext dataContext;
    @Autowired private TaskEntityQueryService entityQueryService;

    @Autowired private ApplicationEventPublisher eventPublisher;

    /**
     * Creates a DTO model of a persisted Task entity.
     *
     * @param taskEntity The Task entity to create a DTO from.
     * @return The Task DTO object.
     */
    public static Task toDTO(TaskEntity taskEntity) {
        return Task.builder()
                .id(taskEntity.id())
                .name(taskEntity.name())
                .description(taskEntity.description())
                .duration(taskEntity.duration())
                .parentLinks(taskEntity.parentLinks().stream()
                        .map(EntityMapper::toDTO)
                        .toList())
                .childLinks(taskEntity.childLinks().stream()
                        .map(EntityMapper::toDTO)
                        .toList())
                .build();
    }

    /**
     * Merge a detached or transient Task object to a persistence-managed entity.
     *
     * @param task The Task object to be persisted or merged.
     * @return The persisted Task entity.
     */
    public TaskEntity toEntity(Task task) {
        TaskEntity taskEntity = task.id() == null ?
                dataContext.createTask(TaskEntity.builder()
                        .name(task.name())
                        .build()) :
                entityQueryService.getTask(task.id());

        return taskEntity.toBuilder()
                .name(Objects.requireNonNullElse(
                        task.name(), taskEntity.name()))
                .duration(Objects.requireNonNullElse(
                        task.duration(), taskEntity.duration()))
                .description(Objects.requireNonNullElse(
                        task.description(), taskEntity.description()))
                .build();
    }

    /**
     * Creates a DTO model of a persisted TaskLink entity.
     *
     * @param linkEntity The TaskLink entity to create a DTO from.
     * @return The TaskLink DTO object.
     */
    public static TaskLink toDTO(TaskLinkEntity linkEntity) {
        return new TaskLink(
                linkEntity.id(),
                linkEntity.priority(),
                linkEntity.position(),
                idOrNull(linkEntity.parent()),
                idOrNull(linkEntity.child()));
    }

    private static Long idOrNull(AbstractEntity entity) {
        return entity == null ? null : entity.id();
    }
}
