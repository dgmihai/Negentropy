package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.backend.entity.TotalDurationEstimate;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class DataContextImpl implements DataContext {
    private static final Logger logger = LoggerFactory.getLogger(DataContextImpl.class);

    @Autowired private EntityQueryService entityQueryService;

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private TagRepository tagRepository;

    @PostConstruct
    public void cleanup() {
        this.deleteAllOrphanedTags();
    }

    @Override
    public void addToTimeEstimateOfAllAncestors(Duration change, TaskID descendantId) {
        entityQueryService.findAncestorLinks(descendantId, null)
                .map(TaskLink::child)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(task -> {
                    this.addToTimeEstimateOfTask(change, ID.of(task));
                });
    }

    @Override
    public void addToTimeEstimateOfTask(Duration change, TaskID taskId) {
        logger.trace("Adding to " + change + " to " + entityQueryService.getTask(taskId));
        TotalDurationEstimate timeEstimate = entityQueryService.getTotalDuration(taskId);
        timeEstimate.totalDuration(timeEstimate.totalDuration().plus(change));
    }

    @Override
    public TaskEntity mergeTask(Task task) {
        TaskEntity taskEntity;

        if (task.id() == null) {
            if (task.name() == null) {
                throw new IllegalArgumentException("New task must have a non-blank unique name");
            }
            logger.debug("Creating new task: " + task.name());

            taskEntity = taskRepository.save(new TaskEntity()
                    .name(task.name()));
            taskEntity.timeEstimates().add(new TotalDurationEstimate(
                    taskEntity,
                    0,
                    Objects.requireNonNullElse(
                            task.duration(), Duration.ZERO)));
        } else {
            taskEntity = entityQueryService.getTask(task.id());
            logger.debug("Merging to existing task: " + taskEntity.name());

            if (task.duration() != null && !taskEntity.duration().equals(task.duration())) {
                Duration change = task.duration().minus(taskEntity.duration());
                this.addToTimeEstimateOfAllAncestors(change, task.id());
            }
        }

        Set<TagEntity> tagEntities = task.tags() == null ?
                taskEntity.tags() :
                task.tags().stream()
                        .map(tag ->entityQueryService.getTag(tag.id()))
                        .collect(Collectors.toSet());

        return taskEntity
                .name(Objects.requireNonNullElse(
                        task.name(), taskEntity.name()))
                .duration(Objects.requireNonNullElse(
                        task.duration(), taskEntity.duration()))
                .description(Objects.requireNonNullElse(
                        task.description(), taskEntity.description()))
                .block(Objects.requireNonNullElse(
                        task.block(), taskEntity.block()))
                .childLinks(taskEntity.childLinks())
                .parentLinks(taskEntity.parentLinks())
                .tags(tagEntities);
    }

    @Override
    public TaskLink mergeNode(TaskNode node) {
        TaskLink linkEntity = entityQueryService.getLink(node.linkId());
        return linkEntity
                .importance(Objects.requireNonNullElse(
                        node.importance(), linkEntity.importance()))
                .recurring(Objects.requireNonNullElse(
                        node.recurring(), linkEntity.recurring()))
                .completed(Objects.requireNonNullElse(
                        node.completed(), linkEntity.completed()));
    }

    @Override
    public TaskLink mergeNode(TaskNodeDTO node) {
        if (node.childId() == null) {
            throw new IllegalArgumentException("Cannot provide a null child ID when merging a task node.");
        }
        TaskEntity child = entityQueryService.getTask(node.childId());
        TaskEntity parent = node.parentId() == null ?
                null:
                entityQueryService.getTask(node.parentId());

        if (child.equals(parent)) {
            throw new IllegalArgumentException("Cannot add task as a child of self");
        }

        if (entityQueryService.findAncestorTasks(ID.of(parent), null)
                .filter(Objects::nonNull)
                .anyMatch(task -> task.equals(parent) || task.equals(child))) {
            throw new IllegalArgumentException("Cannot create link between " + parent + " and " + child +
                    "; would create cyclical hierarchy.");
        }

        Integer position = node.position();

        logger.debug("Inserting task " + child + " as subtask of parent " + parent + " at position " + position);

        if (node.position() == null || node.position() == -1) {
            position = parent == null ?
                    entityQueryService.findChildCount(null, null) :
                    parent.childLinks().size();
        } else {
            List<TaskLink> childLinks = parent == null ?
                    entityQueryService.findChildLinks(null, null).toList() :
                    parent.childLinks();
            for (TaskLink childLink : childLinks) {
                if (childLink.position() >= position) {
                    childLink.position(childLink.position() + 1);
                }
            }
        }

        int importance = node.importance() == null ?
                0 :
                node.importance();
        boolean recurring = node.recurring() != null && node.recurring();
        boolean completed = node.completed() != null && node.completed();

        TaskLink link = linkRepository.save(new TaskLink(
                parent,
                child,
                position,
                importance,
                recurring,
                completed));

        if (parent != null) {
            Duration change = entityQueryService.getTotalDuration(ID.of(child)).totalDuration();
            TaskID parentId = ID.of(parent);

            this.addToTimeEstimateOfAllAncestors(change, parentId);
            try {
                parent.childLinks().add(link.position(), link);
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Specified position for inserting task link is invalid", e);
            }
        }

        child.parentLinks().add(link);

        return link;
    }

    @Override
    public TagEntity mergeTag(TagEntity tagEntity) {
        return tagRepository.save(tagEntity);
    }

    @Override
    public TagEntity mergeTag(Tag tag) {
        TagEntity tagEntity = tag.id() != null ?
                entityQueryService.getTag(tag.id()) :
                new TagEntity();

        return this.mergeTag(tagEntity
                .name(tag.name()));
    }

    @Override
    public void deleteLink(TaskLink link) {
        TaskEntity parent = link.parent();
        TaskEntity child = link.child();

        logger.debug("Deleting link " + link);

        if (parent != null) {
            parent.childLinks().remove(link);

            for (TaskLink childLink : parent.childLinks()) {
                if (childLink.position() > link.position()) {
                    childLink.position(childLink.position() - 1);
                }
            }

        }

        child.parentLinks().remove(link);

        linkRepository.delete(link);


        Duration change = entityQueryService.getTotalDuration(ID.of(child)).totalDuration()
            .negated();
        TaskID parentId = ID.of(parent);
        this.addToTimeEstimateOfAllAncestors(change, parentId);
    }

    @Override
    public void deleteAllOrphanedTags() {
        tagRepository.deleteAllInBatch(entityQueryService.findOrphanedTags().toList());
    }

    @Override
    @VisibleForTesting
    public TaskEntity TESTONLY_mergeTask(TaskEntity taskEntity) {
        return taskRepository.save(taskEntity);
    }

    @Override
    @VisibleForTesting
    public TaskLink TESTONLY_mergeLink(TaskLink link) {
        return linkRepository.save(link);
    }
}
