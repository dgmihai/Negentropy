package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.data.HasTaskData.TaskTemplateData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.*;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.routine.*;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineNodeStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineTaskStep;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class DataContextImpl implements DataContext {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationService netDurationService;

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private TenetRepository tenetRepository;
    @Autowired private MoodRepository moodRepository;

    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;

    @PostConstruct
    public void onStart() {
        this.deleteAllOrphanedTags();
    }

    @Override
    public void addToNetDurationOfAllAncestors(Duration change, TaskID descendantId) {
        entityQueryService.findAncestorLinks(descendantId, null)
                .map(TaskLink::child)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(task -> {
                    this.addToNetDurationOfTask(change, ID.of(task));
                });
    }

    @Override
    public void addToNetDurationOfTask(Duration change, TaskID taskId) {
        log.trace("Adding to " + change + " to " + entityQueryService.getTask(taskId));
        NetDuration netDuration = netDurationService.getNetDurationEntity(taskId);
        netDuration.val(netDuration.val().plus(change));
    }

    @Override
    public TaskEntity mergeTask(Task task) {
        TaskEntity taskEntity;

        if (task.id() == null) {
            if (task.name() == null) {
                throw new IllegalArgumentException("New task must have a non-blank unique name");
            }
            log.debug("Creating new task: " + task.name());

            taskEntity = taskRepository.save(new TaskEntity()
                    .name(task.name()));

            taskEntity.netDurations().add(new NetDuration(
                    taskEntity,
                    0,
                    Objects.requireNonNullElse(
                            task.duration(), Duration.ZERO)));
        } else {
            taskEntity = entityQueryService.getTask(task.id());
            log.debug("Merging to existing task: " + taskEntity.name());

            if (task.duration() != null && !taskEntity.duration().equals(task.duration())) {
                Duration change = task.duration().minus(taskEntity.duration());
                this.addToNetDurationOfAllAncestors(change, task.id());
            }
        }

        Set<TagEntity> tagEntities = task.tags() == null ?
                taskEntity.tags() :
                task.tags().stream()
                        .map(this::mergeTag)
                        .collect(Collectors.toSet());

        taskEntity
                .name(Objects.requireNonNullElse(
                        task.name(), taskEntity.name()))
                .duration(Objects.requireNonNullElse(
                        task.duration(), taskEntity.duration()))
                .description(Objects.requireNonNullElse(
                        task.description(), taskEntity.description()))
                .project(Objects.requireNonNullElse(
                        task.project(), taskEntity.project()))
                .required(Objects.requireNonNullElse(
                        task.required(), taskEntity.required()))
                .childLinks(taskEntity.childLinks())
                .parentLinks(taskEntity.parentLinks())
                .tags(tagEntities);

        log.debug("Merged task: " + taskEntity);
        return taskEntity;
    }

    @Override
    public TaskEntity mergeTaskTemplate(TaskID id, TaskTemplateData<Task, Tag> template) {
        Task task = new Task(
                id,
                null,
                template.description(),
                template.duration(),
                template.required(),
                template.project(),
                template.tags());
        return this.mergeTask(task);
    }


    @Override
    public TaskLink mergeNode(TaskNode node) {
        TaskLink linkEntity = entityQueryService.getLink(node.linkId());

        boolean cronChanged = (node.cron() != null && !(Objects.equals(linkEntity.cron(), node.cron())));
        boolean isBeingCompleted = (node.completed() != null && !linkEntity.completed() && node.completed());

        linkEntity
                .importance(Objects.requireNonNullElse(
                        node.importance(), linkEntity.importance()))
                .recurring(Objects.requireNonNullElse(
                        node.recurring(), linkEntity.recurring()))
                .completed(Objects.requireNonNullElse(
                        node.completed(), linkEntity.completed()))
                .cron(Optional.ofNullable(node.cron()).orElse((linkEntity.cron())))
                .projectDuration(Optional.ofNullable(
                        node.projectDuration())
                        .orElse(linkEntity.projectDuration()));

        boolean hasCron = linkEntity.cron() != null;

        if (cronChanged && hasCron) {
            linkEntity.scheduledFor(DataContext.now());
        }
        if (isBeingCompleted && linkEntity.recurring()) {
            log.debug("Updating scheduled time");
            if (hasCron) {
                linkEntity.scheduledFor(linkEntity.cron().next(DataContext.now()));
            }
            linkEntity.completed(false);
        }
        
        log.debug("Merged task link from node: " + linkEntity);
        return linkEntity;
    }

    @Override
    public TaskLink mergeNode(TaskNodeDTO node) {
        if (node.childId() == null) {
            throw new IllegalArgumentException("Cannot provide a null child ID when merging a task node.");
        }
        TaskEntity child = entityQueryService.getTask(node.childId());
        TaskEntity parent = node.parentId() == null ?
                null :
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

        log.debug("Inserting task " + child + " as subtask of parent " + parent + " at position " + position);

        if (position == null || position == -1) {
            position = parent == null ?
                    entityQueryService.findChildCount(null, null) :
                    parent.childLinks().size();
            log.trace("Position: " + position);
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
        boolean completed = node.completed() != null && (node.completed() && !recurring);

        String cron = node.cron() == null ?
                null :
                node.cron().toString();

        LocalDateTime now = DataContext.now();
        LocalDateTime scheduledFor = now;
        if (cron != null) {
            scheduledFor = node.cron().next(now);
        }

        Duration projectDuration = node.projectDuration();

        TaskLink link = linkRepository.save(new TaskLink(
                null,
                parent,
                child,
                position,
                importance,
                now,
                completed,
                recurring,
                cron,
                scheduledFor,
                projectDuration));

        if (parent != null) {
            Duration change = netDurationService.getNetDuration(child, null);
            TaskID parentId = ID.of(parent);

            this.addToNetDurationOfAllAncestors(change, parentId);
            try {
                parent.childLinks().add(link.position(), link);
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Specified position for inserting task link is invalid", e);
            }
        }

        child.parentLinks().add(link);

        log.debug("Merged task link from node dto: " + link);
        return link;
    }

    @Override
    public TaskLink mergeNodeTemplate(LinkID linkId, TaskNodeTemplateData<?> nodeTemplate) {
        TaskLink link = entityQueryService.getLink(linkId);
        return this.mergeNode(new TaskNode(
                linkId,
                link.parentId(),
                this.toDO(link.child()),
                link.position(),
                nodeTemplate.importance(),
                link.createdAt(),
                nodeTemplate.completed(),
                nodeTemplate.recurring(),
                nodeTemplate.cron(),
                link.scheduledFor(),
                link.projectDuration()));
    }


    @Override
    public TagEntity mergeTag(TagEntity tagEntity) {
        return tagRepository.save(tagEntity);
    }

    @Override
    public TagEntity mergeTag(Tag tag) {
        TagEntity tagEntity = tag.id() != null
                ? entityQueryService.getTag(tag.id())
                : new TagEntity();

        return this.mergeTag(tagEntity
                .name(tag.name()));
    }

    @Override
    public TenetEntity mergeTenet(Tenet tenet) {
        TenetEntity tenetEntity = tenet.id() != null
                ? tenetRepository.getReferenceById(tenet.id())
                : new TenetEntity();

        return tenetRepository.save(tenetEntity.body(tenet.body()));
    }

    @Override
    public MoodEntity mergeMood(Mood mood) {
        MoodEntity moodEntity = mood.id() != null
                ? moodRepository.getReferenceById(mood.id())
                : new MoodEntity();

        return moodRepository.save(moodEntity
                .emotion(mood.emotion())
                .timestamp(mood.timestamp()));
    }

    @Override
    public void deleteLink(TaskLink link) {
        TaskEntity parent = link.parent();
        TaskEntity child = link.child();

        log.debug("Deleting link " + link);

        if (parent != null) {
            parent.childLinks().remove(link);

            for (TaskLink childLink : parent.childLinks()) {
                if (childLink.position() > link.position()) {
                    childLink.position(childLink.position() - 1);
                }
            }

        }

        child.parentLinks().remove(link);

        routineStepRepository.findAll(QRoutineStepEntity.routineStepEntity.link.eq(link))
                .forEach(step -> {
                    step.link(null);
                    step.deletedLink(true);
                });

        linkRepository.delete(link);

        Duration change = netDurationService.getNetDuration(child, null)
            .negated();
        TaskID parentId = ID.of(parent);
        this.addToNetDurationOfAllAncestors(change, parentId);
    }

    @Override
    public void deleteTenet(Long id) {
        tenetRepository.deleteById(id);
    }

    @Override
    public void deleteMood(Long id) {
        moodRepository.deleteById(id);
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

    @Override
    @VisibleForTesting
    public RoutineEntity TESTONLY_mergeRoutine(RoutineEntity routineEntity) {
        return routineRepository.save(routineEntity);
    }

    @Override
    public RoutineStepEntity TESTONLY_mergeRoutineStep(RoutineStepEntity routineStepEntity) {
        return routineStepRepository.save(routineStepEntity);
    }

    @Override
    public Tag toDO(TagEntity tagEntity) {
        return new Tag(
                ID.of(tagEntity),
                tagEntity.name());
    }

    @Override
    public Task toDO(TaskEntity taskEntity) {
        return new Task(ID.of(taskEntity),
                taskEntity.name(),
                taskEntity.description(),
                taskEntity.duration(),
                taskEntity.required(),
                taskEntity.project(),
                taskEntity.tags().stream()
                        .map(this::toDO)
                        .collect(Collectors.toSet()));
    }

    @Override
    public TaskNode toDO(TaskLink link) {
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

    @Override
    public Routine toDO(RoutineEntity routineEntity) {
        return new Routine(
                ID.of(routineEntity),
                routineEntity.children().stream()
                        .map(this::toDO)
                        .toList(),
                routineEntity.currentPosition(),
                routineEntity.estimatedDuration(),
                routineEntity.estimatedDurationLastUpdatedTime(),
                routineEntity.status(),
                routineEntity.syncId());
    }

    @Override
    public RoutineStep toDO(RoutineStepEntity routineStepEntity) {
        RoutineStep result = routineStepEntity.link().isPresent()
                ? new RoutineNodeStep(toDO(routineStepEntity.link().get()))
                : new RoutineTaskStep(toDO(routineStepEntity.task()));

        return result
                .id(ID.of(routineStepEntity))
                .routineId(ID.of(routineStepEntity.routine()))
                .parentId(ID.of(routineStepEntity.parentStep()))
                .children(routineStepEntity.children().stream()
                        .map(this::toDO)
                        .toList())
                .startTime(routineStepEntity.startTime())
                .finishTime(routineStepEntity.finishTime())
                .lastSuspendedTime(routineStepEntity.lastSuspendedTime())
                .elapsedSuspendedDuration(routineStepEntity.elapsedSuspendedDuration())
                .status(routineStepEntity.status());
    }

    @Override
    public Tenet toDO(TenetEntity tenetEntity) {
        return new Tenet(
                tenetEntity.id(),
                tenetEntity.body());
    }

    @Override
    public Mood toDO(MoodEntity moodEntity) {
        return new Mood(
                moodEntity.id(),
                moodEntity.emotion(),
                moodEntity.timestamp());
    }
}
