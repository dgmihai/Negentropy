package com.trajan.negentropy.server.backend;

import com.querydsl.core.BooleanBuilder;
import com.trajan.negentropy.server.backend.entity.*;
import com.trajan.negentropy.server.backend.repository.*;
import com.trajan.negentropy.server.backend.util.DFS;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service("entityQueryService")
@Transactional
public class EntityQueryServiceImpl implements EntityQueryService {
    private static final Logger logger = LoggerFactory.getLogger(EntityQueryServiceImpl.class);

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private TotalDurationEstimateRepository durationEstimateRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository stepRepository;
    
    private static final QTaskLink Q_LINK = QTaskLink.taskLink;
    private static final QTaskEntity Q_TASK = QTaskEntity.taskEntity;

    private Map<TaskID, Duration> cachedTotalDurations = new HashMap<>();
    private TaskFilter activeFilter = new TaskFilter();

    @Override
    public TaskEntity getTask(TaskID taskId) {
        return taskRepository.findById(taskId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get task with ID: " + taskId));
    }

    @Override
    public TaskLink getLink(LinkID linkId) {
        return linkRepository.findById(linkId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get link with ID: " + linkId));
    }

    @Override
    public TagEntity getTag(TagID tagId) {
        return tagRepository.findById(tagId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get tag with ID: " + tagId));
    }

    @Override
    public RoutineEntity getRoutine(RoutineID routineId) {
        logger.trace("getRoutine");
        return routineRepository.findById(routineId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get routine with ID: " + routineId));
    }

    @Override
    public RoutineStepEntity getRoutineStep(StepID stepId) {
        return stepRepository.findById(stepId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get step with ID: " + stepId));
    }
    
    @Override
    public Optional<TagEntity> findTag(String name) {
        return tagRepository.findFirstByName(name);
    }

    private BooleanBuilder filterLink(TaskFilter filter) {
        BooleanBuilder builder = new BooleanBuilder();
        if (filter != null) {
            if (filter.importanceThreshold() != null) {
                builder.and(Q_LINK.importance.loe(filter.importanceThreshold()));
            }

            if (filter.availableAtTime() != null) {
                builder.and(Q_LINK.scheduledFor.loe(filter.availableAtTime()));
            }

            if (filter.showCompleted() != null && !filter.showCompleted()) {
                builder.andNot(Q_LINK.completed.eq(true));
            }
       }
        return builder;
    }

    private BooleanBuilder filterTask(TaskFilter filter, QTaskEntity qTask) {
        BooleanBuilder builder = new BooleanBuilder();
        if (filter != null) {

            // Filter by name, case-insensitive
            if (filter.name() != null && !filter.name().isBlank()) {
                builder.and(qTask.name.lower().contains(filter.name().toLowerCase()));
            }

            // Filter if task is a block
            if (filter.showOnlyBlocks() != null && filter.showOnlyBlocks()) {
                builder.and(qTask.block.eq(true));
            }

            if (filter.includeParents() != null && filter.includeParents()) {
                builder.or(qTask.childLinks.isNotEmpty());
            }

            // Filter by included task IDs, and if this filter is by inner join or not
            if (filter.includedTagIds() != null && !filter.includedTagIds().isEmpty()) {
                Consumer<TagEntity> filterFunction =
                        (filter.innerJoinIncludedTags() != null && filter.innerJoinIncludedTags()) ?
                        tagEntity -> builder.and(qTask.tags.contains(tagEntity)) :
                        tagEntity -> builder.or(qTask.tags.contains(tagEntity));

                filter.includedTagIds().stream()
                        .map(this::getTag)
                        .forEach(filterFunction);
            }

            // Filter by tags that must be excluded
            if (filter.excludedTagIds() != null && !filter.excludedTagIds().isEmpty()) {
                filter.excludedTagIds().stream()
                        .map(this::getTag)
                        .forEach(tagEntity -> builder.and(qTask.tags.contains(tagEntity).not()));
            }
        }

        return builder;
    }

    private BooleanBuilder filter(TaskFilter filter, QTaskEntity qTask) {
        return this.filterLink(filter).and(
            this.filterTask(filter, qTask));
    }

    @Override
    public Stream<TaskEntity> findTasks(TaskFilter filter) {
        return StreamSupport.stream(taskRepository.findAll(this.filterTask(filter, Q_TASK))
                        .spliterator(), false);
    }

    private BooleanBuilder byParent(TaskID taskId, TaskFilter filter) {
        return this.filter(filter, Q_LINK.child)
                .and(taskId == null ?
                        Q_LINK.parent.isNull() :
                        Q_LINK.parent.id.eq(taskId.val()));
    }

    private BooleanBuilder byChild(TaskID taskId, TaskFilter filter) {
        return this.filter(filter, Q_LINK.parent)
                .and(taskId == null ?
                        Q_LINK.child.isNull() :
                        Q_LINK.child.id.eq(taskId.val()));
    }

    @Override
    public int findChildCount(TaskID parentId, TaskFilter filter) {
        return (int) linkRepository.count(byParent(parentId, filter));
    }

    @Override
    public int findChildCount(TaskID parentId, TaskFilter filter, int offset, int limit) {
        return (int) this.findChildLinks(parentId, filter, offset, limit).count();
    }

    @Override
    public boolean hasChildren(TaskID parentId, TaskFilter filter) {
        return linkRepository.exists(byParent(parentId, filter));
    }

    @Override
    public Stream<TaskLink> findChildLinks(TaskID parentId, TaskFilter filter) {
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(byParent(parentId, filter), sort)
                .spliterator(), false);
  }

    @Override
    public Stream<TaskLink> findChildLinks(TaskID parentId, TaskFilter filter, int offset, int limit) {
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(
                byParent(parentId, filter),
                        new OffsetBasedPageRequest(offset, limit, sort))
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findChildTasks(TaskID parentId, TaskFilter filter) {
        return this.findChildLinks(parentId, filter).map(TaskLink::child);
    }

    @Override
    public Stream<TaskLink> findParentLinks(TaskID childId, TaskFilter filter) {
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(byChild(childId, filter), sort)
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findParentTasks(TaskID childId, TaskFilter filter) {
        return this.findParentLinks(childId, filter).map(TaskLink::parent);
    }

    @Override
    public Stream<TaskLink> findAncestorLinks(TaskID descendantId, TaskFilter filter) {
        return DFS.getLinks(descendantId,
                childId -> this.findParentLinks(childId, filter),
                link -> ID.of(link.parent()))
                .stream();
    }

    @Override
    public Stream<TaskEntity> findAncestorTasks(TaskID descendantId, TaskFilter filter) {
        return this.findAncestorLinks(descendantId, filter).map(TaskLink::parent);
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskFilter filter) {
        return DFS.getLinks(ancestorId,
                parentId -> this.findChildLinks(parentId, filter),
                link -> ID.of(link.child()))
                .stream();
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskFilter filter, Consumer<TaskLink> consumer) {
        return DFS.getLinks(ancestorId,
                        parentId -> this.findChildLinks(parentId, filter),
                        link -> ID.of(link.child()),
                        consumer)
                .stream();
    }

    @Override
    public Stream<TaskEntity> findDescendantTasks(TaskID ancestorId, TaskFilter filter) {
        return this.findDescendantLinks(ancestorId, filter).map(TaskLink::child);
    }

    @Override
    public TotalDurationEstimate getTotalDuration(TaskID taskId) {
        // TODO: Implement importance in time estimate caching
        return this.getTotalDuration(taskId, 0);
    }

    @Override
    public TotalDurationEstimate getTotalDuration(TaskID taskId, int importance) {
        TaskEntity task = this.getTask(taskId);
        return task.timeEstimates().stream()
                .filter(
                timeEstimate -> importance == timeEstimate.importance())
                .findFirst()
                .orElseThrow();
    }

    @Override
    public Stream<TotalDurationEstimate> getTotalDurationWithImportanceThreshold(TaskID taskId, int importanceDifference) {
        TaskEntity task = this.getTask(taskId);
        int lowestImportance = this.getLowestImportanceOfDescendants(taskId);
        return task.timeEstimates().stream()
                .filter(timeEstimate -> {
                    int difference = lowestImportance - importanceDifference;
                    return timeEstimate.importance() <= difference;
                });
    }

    @Override
    public Duration calculateTotalDuration(TaskID taskId, TaskFilter filter) {
        if (filter == null) {
            return getTotalDuration(taskId).totalDuration();
        }

        if (filter.name().isBlank() && filter.includedTagIds().isEmpty() && filter.excludedTagIds().isEmpty()) {
            return getTotalDuration(taskId, filter.importanceThreshold()).totalDuration();
        }

        boolean filterCached = filter.equals(activeFilter);

        if (!filterCached) {
            cachedTotalDurations.clear();
        }

        Predicate<TaskID> hasCachedNetDuration =
                id -> filterCached && cachedTotalDurations.containsKey(id);

        if (hasCachedNetDuration.test(taskId)) {
            return cachedTotalDurations.get(taskId);
        }

        Stream<TaskEntity> descendants = this.findDescendantTasks(taskId, filter);
        LinkedList<TaskEntity> descendantStack = new LinkedList<>();
        descendants.takeWhile(t -> hasCachedNetDuration.test(ID.of(t)))
                .forEachOrdered(descendantStack::push);

        TaskEntity task = this.getTask(taskId);
        if (descendantStack.isEmpty()) {
            return this.getTask(taskId).duration();
        }

        Duration durationSum = cachedTotalDurations.get(ID.of(descendantStack.poll()));
        TaskEntity next = descendantStack.poll();
        while (next != null) {
            durationSum = durationSum.plus(next.duration());
            cachedTotalDurations.put(ID.of(next), durationSum);
            next = descendantStack.poll();
        }

        cachedTotalDurations.put(taskId, durationSum.plus(task.duration()));
        return durationSum;
    }

    @Override
    public int getLowestImportanceOfDescendants(TaskID ancestorId) {
        QTotalDurationEstimate qTotalDurationEstimate = QTotalDurationEstimate.totalDurationEstimate;
        return durationEstimateRepository.findOne(
                qTotalDurationEstimate.task.id.eq(ancestorId.val())).orElseThrow()
                .importance();
    }

    @Override
    public Stream<TaskLink> findLeafTaskLinks(TaskFilter filter) {
        return StreamSupport.stream(linkRepository.findAll(
                Q_LINK.child.childLinks.isEmpty()
                        .and(this.filterLink(filter)))
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findOrphanedTasks() {
        QTaskEntity qTask = QTaskEntity.taskEntity;
        return StreamSupport.stream(taskRepository.findAll(
                qTask.parentLinks.isEmpty())
                .spliterator(), false);
    }

    @Override
    public Stream<TagEntity> findOrphanedTags() {
        QTagEntity qTag = QTagEntity.tagEntity;
        return StreamSupport.stream(tagRepository.findAll(
                        qTag.tasks.isEmpty())
                .spliterator(), false);
    }
}
