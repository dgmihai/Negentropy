package com.trajan.negentropy.server.backend;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedListMultimap;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.*;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.*;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.routine.*;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineNodeStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineTaskStep;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.server.backend.repository.*;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.util.SpringContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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
    @Autowired private StressorRepository stressorRepository;

    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;

    @PostConstruct
    public void onStart() {
        this.deleteAllOrphanedTags();
    }

    @Override
    public TaskEntity merge(Task task) {
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
//            log.debug("Merging to existing task: " + taskEntity.name());
//
//            if (task.duration() != null && !taskEntity.duration().equals(task.duration())) {
//                Duration change = task.duration().minus(taskEntity.duration());
//                this.addToNetDurationOfAllAncestors(change, task.id());
//            }
        }

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
                .difficult(Objects.requireNonNullElse(
                        task.difficult(), taskEntity.difficult()))
                .tags((task.tags() != null)
                    ? task.tags().stream()
                        .map(this::merge)
                        .collect(Collectors.toSet())
                    : taskEntity.tags())
                .childLinks(taskEntity.childLinks())
                .parentLinks(taskEntity.parentLinks());

        log.debug("Merged task: " + taskEntity);
        return taskEntity;
    }

    @Override
    public TaskEntity merge(TaskID id, Task template) {
        Task task = new Task(
                id,
                null,
                template.description(),
                template.duration(),
                template.required(),
                template.project(),
                template.difficult(),
                template.tags());
        return this.merge(task);
    }

    @Override
    public TaskLink merge(TaskNode node) {
        TaskLink linkEntity = entityQueryService.getLink(node.linkId());

        if (node.cron() != null) {
            if (node.cron().equals(K.NULL_CRON_FULL)) {
                linkEntity.cron((CronExpression) null);
                node.cron(null);
            } else {
                linkEntity.cron(node.cron());
            }
        }

        boolean cronChanged = (node.cron() != null && !(Objects.equals(linkEntity.cron(), node.cron())));
        boolean isBeingCompleted = (node.completed() != null && !linkEntity.completed() && node.completed());

        if (node.projectEtaLimit() != null) {
            linkEntity.projectEtaLimit(node.projectEtaLimit());
        }
        if (node.projectStepCountLimit() != null) {
            linkEntity.projectStepCountLimit(node.projectStepCountLimit());
        }
        if (node.projectDurationLimit() != null) {
            linkEntity.projectDurationLimit(node.projectDurationLimit());
        }

        linkEntity
                .importance(Objects.requireNonNullElse(
                        node.importance(), linkEntity.importance()))
                .recurring(Objects.requireNonNullElse(
                        node.recurring(), linkEntity.recurring()))
                .cycleToEnd(Objects.requireNonNullElse(
                        node.cycleToEnd(), linkEntity.cycleToEnd()))
                .completed(Objects.requireNonNullElse(
                        node.completed(), linkEntity.completed()))
                .positionFrozen(Objects.requireNonNullElse(
                        node.positionFrozen(), linkEntity.positionFrozen()));

        boolean hasCron = linkEntity.cron() != null;

        if (cronChanged && hasCron) {
            linkEntity.scheduledFor(DataContext.now());
        }

        if (isBeingCompleted) {
            if (linkEntity.recurring()) {
                log.debug("Updating scheduled time");
                if (hasCron) {
                    linkEntity.scheduledFor(linkEntity.cron().next(DataContext.now()));
                }
                linkEntity.completed(false);

                if (linkEntity.cycleToEnd() && !linkEntity.positionFrozen()) {
                    if (linkEntity.parent() != null) {
                        log.debug("Cycling to end");
                        int position = linkEntity.position();
                        List<TaskLink> childLinks = linkEntity.parent().childLinks();
                        childLinks.remove(linkEntity);

                        for (int i = childLinks.size() - 1; i > 0; i--) {
                            TaskLink current = childLinks.get(i);
                            if (!current.positionFrozen()) {
                                childLinks.add(i, linkEntity);
                                break;
                            }
                        }

                        for (int i = position; i < childLinks.size(); i++) {
                            TaskLink current = childLinks.get(i);
                            current.position(i - 1);
                        }
                        linkEntity.position(position);
                        linkEntity.parent().childLinks(childLinks);
                    }
                }
            }

            log.debug("Saving routine step for tracking");
            routineStepRepository.save(new RoutineStepEntity(linkEntity)
                    .finishTime(DataContext.now())
                    .status(TimeableStatus.COMPLETED));
        }

        log.debug("Merged task link from node: " + linkEntity);
        return linkEntity;
    }

    @Override
    public TaskLink merge(TaskNodeDTO node) {
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

        boolean positionFrozen = node.positionFrozen() != null && node.positionFrozen();
        Integer position = node.position();

        log.debug("Inserting task " + child + " as subtask of parent " + parent + " at position " + position);

        List<TaskLink> childLinks = parent == null ?
                entityQueryService.findChildLinks(null, null).toList() :
                parent.childLinks();

        if (position == null || position == -1) {
            position = parent == null ?
                    entityQueryService.findChildCount(null, null) :
                    parent.childLinks().size();
            log.trace("Position: " + position);
        }

        if (position == 0) {
            // Check for required tasks from the beginning of the list
            for (TaskLink childLink : childLinks) {
                if (childLink.positionFrozen()) {
                    position++;
                } else {
                    break;
                }
            }

            for (int i = position; i < childLinks.size(); i++) {
                TaskLink current = childLinks.get(i);
                current.position(current.position() + 1);
            }
        } else if (position >= childLinks.size()) {
            // Check for required tasks from the end of the list
            for (int i = childLinks.size() - 1; i >= 0; i--) {
                TaskLink current = childLinks.get(i);
                if (current.positionFrozen()) {
                    position--;
                    current.position(current.position() + 1);
                } else {
                    break;
                }
            }

            if (position < 0) {
                position = childLinks.size();
            }
        } else {
            for (int i = position; i < childLinks.size(); i++) {
                TaskLink current = childLinks.get(i);
                current.position(current.position() + 1);
            }
        }

        int importance = node.importance() == null ?
                0 :
                node.importance();
        boolean recurring = node.recurring() != null && node.recurring();
        boolean cycleToEnd = node.cycleToEnd() != null && node.cycleToEnd();
        boolean completed = node.completed() != null && (node.completed() && !recurring);

        String cron = null;
        if (node.cron() != null) {
            if (node.cron().equals(K.NULL_CRON_FULL)) {
                node.cron(null);
            } else {
                cron = node.cron().toString();
            }
        }

        LocalDateTime now = DataContext.now();
        LocalDateTime scheduledFor = now;
        if (cron != null) {
            scheduledFor = recurring
                    ? now
                    : node.cron().next(now);
        }

        Duration projectDurationLimit = (node.projectDurationLimit() != null)
                ? node.projectDurationLimit().orElse(null)
                : null;
        Integer projectStepCountLimit = (node.projectStepCountLimit() != null)
                ? node.projectStepCountLimit().orElse(null)
                : null;
        LocalTime projectEtaLimit = (node.projectEtaLimit() != null)
                ? node.projectEtaLimit().orElse(null)
                : null;
        String projectEtaLimitString = (projectEtaLimit != null)
                ? projectEtaLimit.toString()
                : null;

        TaskLink link = linkRepository.save(new TaskLink(
                null,
                parent,
                child,
                position,
                positionFrozen,
                importance,
                now,
                completed,
                recurring,
                cycleToEnd,
                cron,
                scheduledFor,
                projectDurationLimit,
                projectStepCountLimit,
                projectEtaLimitString));

        if (parent != null) {
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
    public TaskLink merge(LinkID linkId, TaskNodeTemplateData<?> nodeTemplate) {
        TaskLink link = entityQueryService.getLink(linkId);
        return this.merge(new TaskNode(
                linkId,
                link.parentId(),
                this.toLazyDO(link.child()),
                link.position(),
                link.positionFrozen(),
                nodeTemplate.importance(),
                link.createdAt(),
                nodeTemplate.completed(),
                nodeTemplate.recurring(),
                nodeTemplate.cycleToEnd(),
                nodeTemplate.cron(),
                link.scheduledFor(),
                link.projectDurationLimit(),
                link.projectStepCountLimit(),
                link.projectEtaLimit()));
    }


    @Override
    public TagEntity merge(TagEntity tagEntity) {
        return tagRepository.save(tagEntity);
    }

    @Override
    public TagEntity merge(Tag tag) {
        TagEntity tagEntity = tag.id() != null
                ? entityQueryService.getTag(tag.id())
                : new TagEntity();

        return this.merge(tagEntity
                .name(tag.name()));
    }

    @Override
    public TenetEntity merge(Tenet tenet) {
        TenetEntity tenetEntity = tenet.id() != null
                ? tenetRepository.getReferenceById(tenet.id())
                : new TenetEntity();

        return tenetRepository.save(tenetEntity.body(tenet.body()));
    }

    @Override
    public MoodEntity merge(Mood mood) {
        MoodEntity moodEntity = mood.id() != null
                ? moodRepository.getReferenceById(mood.id())
                : new MoodEntity();

        return moodRepository.save(moodEntity
                .emotion(mood.emotion())
                .timestamp(mood.timestamp()));
    }

    @Override
    public StressorEntity merge(Stressor stressor) {
        StressorEntity stressorEntity = stressor.id() != null
                ? stressorRepository.getReferenceById(stressor.id())
                : new StressorEntity();

        return stressorRepository.save(stressorEntity
                .name(Objects.requireNonNullElse(stressor.name(), stressorEntity.name())));
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
                    if (step.routine() != null
                            && (step.routine().status().equals(TimeableStatus.ACTIVE)
                            || step.routine().status().equals(TimeableStatus.NOT_STARTED))) {
                        RoutineService routineService = SpringContext.getBean(RoutineService.class);
                        Change deleteChange = new DeleteChange<>(ID.of(step));
                        routineService.notifyChanges(Request.of(deleteChange),
                                new LinkedMultiValueMap<>());
                    }

                    step.link(null);
                    step.deletedLink(true);
                });

        linkRepository.delete(link);
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
    public void deleteStressor(Long id) {
        stressorRepository.deleteById(id);
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
    public Task toLazyDO(TaskEntity taskEntity) {
        return new Task(ID.of(taskEntity),
                taskEntity.name(),
                taskEntity.description(),
                taskEntity.duration(),
                taskEntity.required(),
                taskEntity.project(),
                taskEntity.difficult(),
                null);
    }

    @Override
    public Task toEagerDO(TaskEntity taskEntity) {
        return toLazyDO(taskEntity)
                .tags(tagRepository.findByTasksId(taskEntity.id())
                        .map(this::toDO)
                        .collect(Collectors.toSet()));
    }

    private TaskNode toAbstractDO(TaskLink link) {
        return new TaskNode(
                ID.of(link),
                ID.of(link.parent()),
                null,
                link.position(),
                link.positionFrozen(),
                link.importance(),
                link.createdAt(),
                link.completed(),
                link.recurring(),
                link.cycleToEnd(),
                link.cron(),
                link.scheduledFor(),
                link.projectDurationLimit(),
                link.projectStepCountLimit(),
                link.projectEtaLimit());
    }

    @Override
    public TaskNode toLazyDO(TaskLink link) {
        return toAbstractDO(link)
                .child(toLazyDO(link.child()));
    }

    @Override
    public TaskNode toEagerDO(TaskLink link) {
        return toAbstractDO(link)
                .child(toEagerDO(link.child()));
    }

    @Override
    public Routine toDO(RoutineEntity routineEntity) {
        LinkedList<RoutineStep> children = new LinkedList<>();
        LinkedListMultimap<StepID, StepID> adjacencyMap = LinkedListMultimap.create();
        Map<StepID, RoutineStep> stepMap = new HashMap<>();

        routineEntity.children().forEach(child -> {
            children.add(this.toDO(child));
        });

        routineEntity.getDescendants().stream()
                .map(this::toDO)
                .forEach(step -> {
                    stepMap.put(step.id(), step);
                    adjacencyMap.put(step.parentId(), step.id());
                });

        return new Routine(
                ID.of(routineEntity),
                stepMap,
                adjacencyMap,
                children,
                routineEntity.currentPosition(),
                routineEntity.status(),
                routineEntity.autoSync(),
                routineEntity.syncId());
    }

    @Override
    public RoutineStep toDO(RoutineStepEntity routineStepEntity) {
        RoutineStep result = routineStepEntity.link().isPresent()
                ? new RoutineNodeStep(toLazyDO(routineStepEntity.link().get()))
                : new RoutineTaskStep(toLazyDO(routineStepEntity.task()));

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

    @Override
    public Stressor toDO(StressorEntity stressorEntity) {
        return new Stressor(
                stressorEntity.id(),
                stressorEntity.name());
    }
}
