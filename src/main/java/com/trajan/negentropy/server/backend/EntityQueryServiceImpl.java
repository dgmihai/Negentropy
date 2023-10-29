package com.trajan.negentropy.server.backend;

import com.querydsl.core.BooleanBuilder;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.*;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.QNetDuration;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.server.backend.repository.*;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QSort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Benchmark(millisFloor = 10)
public class EntityQueryServiceImpl implements EntityQueryService {
    private static final Logger logger = LoggerFactory.getLogger(EntityQueryServiceImpl.class);

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private NetDurationRepository netDurationRepository;
    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository stepRepository;

    private static final com.trajan.negentropy.model.entity.QTaskLink Q_LINK = com.trajan.negentropy.model.entity.QTaskLink.taskLink;
    private static final QTaskEntity Q_TASK = QTaskEntity.taskEntity;

    private Map<TaskID, Duration> cachedTotalDurations = new HashMap<>();
    private TaskTreeFilter activeFilter = new TaskTreeFilter();

    @Override
    public TaskEntity getTask(@NotNull TaskID taskId) {
        if (taskId == null) throw new IllegalArgumentException("Task ID cannot be null.");
        return taskRepository.findById(taskId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get task with ID: " + taskId));
    }

    @Override
    public Stream<TaskEntity> getTasks(Collection<TaskID> taskIds) {
        return taskRepository.findAllById(taskIds.stream()
                        .map(ID::val)
                        .collect(Collectors.toSet()))
                .stream();
    }

    @Override
    public TaskLink getLink(@NotNull LinkID linkId) {
        if (linkId == null) throw new IllegalArgumentException("Link ID cannot be null.");
        return linkRepository.findById(linkId.val()).orElseThrow(
                () -> new NoSuchElementException("Failed to get link with ID: " + linkId));
    }

    @Override
    public Stream<TaskLink> getLinks(Collection<LinkID> linkIds) {
        return linkRepository.findAllById(linkIds.stream()
                        .map(ID::val)
                        .collect(Collectors.toSet()))
                .stream();
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

    @Override
    public BooleanBuilder filterLinkPredicate(TaskNodeTreeFilter filter) {
        BooleanBuilder builder = new BooleanBuilder();
        if (filter != null) {

            if (filter.importanceThreshold() != null) {
                builder.and(Q_LINK.importance.loe(filter.importanceThreshold()));
            }

            if (!(filter.ignoreScheduling() != null && filter.ignoreScheduling())) {
                builder.and(Q_LINK.scheduledFor.loe(filter.availableAtTime() != null
                        ? filter.availableAtTime()
                        : LocalDateTime.now()));
            }

            if (filter.completed() != null) {
                builder.and(Q_LINK.completed.eq(filter.completed()));
            }

            if (filter.recurring() != null) {
                builder.and(Q_LINK.recurring.eq(filter.recurring()));
            }
       }
        return builder.and(filterTaskPredicate(filter, Q_LINK.child));
    }

    @Override
    public BooleanBuilder filterTaskPredicate(TaskTreeFilter filter, QTaskEntity qTask) {
        BooleanBuilder builder = new BooleanBuilder();
        if (filter != null) {

            // Filter by name, case-insensitive
            if (filter.name() != null && !filter.name().isBlank()) {
                builder.and(qTask.name.lower().contains(filter.name().toLowerCase()));
            }

            // Filter out if task isn't required
            if (filter.options().contains(TaskTreeFilter.ONLY_REQUIRED)) {
                builder.and(qTask.required.isTrue());
            }

            // Filter out if task isn't a project
            if (filter.options().contains(TaskTreeFilter.ONLY_PROJECTS)) {
                builder.and(qTask.project.isTrue());
            }

            if (filter.options().contains(TaskTreeFilter.ALWAYS_INCLUDE_PARENTS)) {
                if (filter.options().size() > 1) {
                    // 'or' doesn't work well if it's the only option
                    builder.or(qTask.childLinks.isNotEmpty());
                }
            }

            // Filter by included task IDs, and if this filter is by inner join or not
            if (filter.includedTagIds() != null && !filter.includedTagIds().isEmpty()) {
                List<TagEntity> includedTags = tagRepository.findByIdIn(filter.includedTagIds()
                        .stream().map(ID::val).toList());
                Consumer<TagEntity> filterFunction =
                        (filter.options().contains(TaskTreeFilter.INNER_JOIN_INCLUDED_TAGS)) ?
                                tagEntity -> builder.and(qTask.tags.contains(tagEntity)) :
                                tagEntity -> builder.or(qTask.tags.contains(tagEntity));

                includedTags.forEach(filterFunction);
            }

            // Filter by tags that must be excluded
            if (filter.excludedTagIds() != null && !filter.excludedTagIds().isEmpty()) {
                List<TagEntity> excludedTags = tagRepository.findByIdIn(filter.excludedTagIds()
                        .stream().map(ID::val).toList());
                excludedTags.forEach(tagEntity -> builder.and(qTask.tags.contains(tagEntity).not()));
            }
        }

        return builder;
    }

    private BooleanBuilder filter(TaskNodeTreeFilter filter, QTaskEntity qTask) {
        return this.filterLinkPredicate(filter).and(
            this.filterTaskPredicate(filter, qTask));
    }

    @Override
    public Stream<TaskEntity> findTasks(TaskTreeFilter filter) {
        if (filter instanceof TaskNodeTreeFilter nodeFilter) {
            return StreamSupport.stream(linkRepository.findAll(this.filterLinkPredicate(nodeFilter))
                    .spliterator(), true)
                    .unordered()
                    .map(TaskLink::child)
                    .distinct();
        } else {
            return StreamSupport.stream(taskRepository.findAll(this.filterTaskPredicate(filter, Q_TASK))
                    .spliterator(), true)
                    .unordered();
        }
    }

    @Override
    public Stream<TaskLink> findLinks(TaskNodeTreeFilter filter) {
        return StreamSupport.stream(linkRepository.findAll(this.filterLinkPredicate(filter))
                .spliterator(), true)
                .unordered();
    }

    @Override
    public Stream<LinkID> findLinkIds(TaskNodeTreeFilter filter) {
        return findLinks(filter).map(ID::of);
    }

    private BooleanBuilder byParent(TaskID taskId, TaskNodeTreeFilter filter) {
        return this.filter(filter, Q_LINK.child)
                .and(taskId == null ?
                        Q_LINK.parent.isNull() :
                        Q_LINK.parent.id.eq(taskId.val()));
    }

    private BooleanBuilder byChild(TaskID taskId, TaskNodeTreeFilter filter) {
        return this.filter(filter, Q_LINK.parent)
                .and(taskId == null ?
                        Q_LINK.child.isNull() :
                        Q_LINK.child.id.eq(taskId.val()));
    }

    @Override
    public int findChildCount(TaskID parentId, TaskNodeTreeFilter filter) {
        return (int) linkRepository.count(byParent(parentId, filter));
    }

    @Override
    public int findChildCount(TaskID parentId, TaskNodeTreeFilter filter, int offset, int limit) {
        return (int) this.findChildLinks(parentId, filter, offset, limit).count();
    }

    @Override
    public boolean hasChildren(TaskID parentId, TaskNodeTreeFilter filter) {
        return linkRepository.exists(byParent(parentId, filter));
    }

    @Override
    public Stream<TaskLink> findChildLinks(TaskID parentId, TaskNodeTreeFilter filter) {
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(byParent(parentId, filter), sort)
                .spliterator(), false);
  }

    @Override
    public Stream<TaskLink> findChildLinks(TaskID parentId, TaskNodeTreeFilter filter, int offset, int limit) {
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(
                byParent(parentId, filter),
                        new OffsetBasedPageRequest(offset, limit, sort))
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findChildTasks(TaskID parentId, TaskNodeTreeFilter filter) {
        return this.findChildLinks(parentId, filter).map(TaskLink::child);
    }

    @Override
    public Stream<TaskLink> findParentLinks(TaskID childId, TaskNodeTreeFilter filter) {
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(byChild(childId, filter), sort)
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findParentTasks(TaskID childId, TaskNodeTreeFilter filter) {
        return this.findParentLinks(childId, filter).map(TaskLink::parent);
    }

    @Override
    public Stream<TaskLink> findAncestorLinks(TaskID descendantId, TaskNodeTreeFilter filter) {
        return DFSUtil.traverseTaskLinks(
                descendantId,
                link -> ID.of(link.parent()),
                id -> this.findParentLinks(id, filter),
                null);
    }

    @Override
    public Stream<TaskEntity> findAncestorTasks(TaskID descendantId, TaskNodeTreeFilter filter) {
        return this.findAncestorLinks(descendantId, filter).map(TaskLink::parent);
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskNodeTreeFilter filter) {
        return this.findDescendantLinks(ancestorId, filter, null);
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskNodeTreeFilter filter, Consumer<TaskLink> consumer) {
        return DFSUtil.traverseTaskLinks(
                ancestorId,
                link -> ID.of(link.child()),
                parentId -> this.findChildLinks(parentId, filter),
                consumer);
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(LinkID ancestorId, TaskNodeTreeFilter filter) {
        return this.findDescendantLinks(ancestorId, filter, null);
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(LinkID ancestorId, TaskNodeTreeFilter filter, Consumer<TaskLink> consumer) {
        TaskLink ancestorLink = this.getLink(ancestorId);

        return DFSUtil.traverseTaskLinks(
                ID.of(ancestorLink.child()),
                link -> ID.of(link.child()),
                parentId -> this.findChildLinks(parentId, filter),
                consumer);
    }

    @Override
    public Stream<TaskLink> findDescendantTasksFromLink(LinkID linkId, TaskNodeTreeFilter filter) {
        return this.findDescendantTasksFromLink(linkId, filter, null);
    }

    @Override
    public Stream<TaskLink> findDescendantTasksFromLink(LinkID linkId, TaskNodeTreeFilter filter, Consumer<TaskLink> consumer) {
        TaskLink rootLink = this.getLink(linkId);
        Stream<TaskLink> siblings = this.findChildLinks(ID.of(rootLink.parent()), filter)
                .filter(link -> link.position() < rootLink.position());

        Stream<TaskLink> resultStream = Stream.of();
        for (TaskLink sibling : siblings.toList()) {
            if (!Objects.equals(sibling.id(), rootLink.id())) {
                resultStream = Stream.concat(
                        resultStream,
                        Stream.of(sibling));
            }
            resultStream = Stream.concat(
                    resultStream,
                    this.findDescendantLinks(ID.of(sibling), filter));
        }

        return resultStream;
        // TODO: Test this
    }

    @Override
    public Stream<TaskEntity> findDescendantTasks(TaskID ancestorId, TaskNodeTreeFilter filter) {
        return this.findDescendantLinks(ancestorId, filter).map(TaskLink::child);
    }

//    @Override
//    public NetDuration getNetDuration(TaskID taskId) {
//        return
//    }
//
//    @Override
//    public NetDuration getNetDuration(TaskID taskId, int importance) {
//        TaskEntity task = this.getTask(taskId);
//        return task.netDurations().stream()
//                .filter(
//                netDuration -> importance == netDuration.importance())
//                .findFirst()
//                .orElseThrow();
//    }

    @Override
    public Stream<NetDuration> getTotalDurationWithImportanceThreshold(TaskID taskId, int importanceDifference) {
        TaskEntity task = this.getTask(taskId);
        int lowestImportance = this.getLowestImportanceOfDescendants(taskId);
        return task.netDurations().stream()
                .filter(netDuration -> {
                    int difference = lowestImportance - importanceDifference;
                    return netDuration.importance() <= difference;
                });
    }

//    @Override
//    public Duration calculateNetDuration(TaskID taskId, TaskNodeTreeFilter filter) {
//        boolean save = false;
//
//        if (filter == null) {
//            try {
//                return netDurationService.getNetDuration(taskId).val();
//            } catch (NoSuchElementException e) {
//                logger.warn("No net duration found for task " + taskId.val() + ", calculating new.");
//                save = true;
//            }
//        } else {
//            if ((filter.name() == null || filter.name().isBlank())
//                    && filter.includedTagIds().isEmpty()
//                    && filter.excludedTagIds().isEmpty()
//                    && filter.importanceThreshold() != null) {
//                return netDurationService.getNetDuration(taskId, filter.importanceThreshold()).val();
//            }
//        }
//
//        boolean filterCached = activeFilter.equals(filter);
//
//        if (!filterCached) {
//            cachedTotalDurations.clear();
//        }
//
//        Predicate<TaskID> hasCachedNetDuration =
//                id -> filterCached && cachedTotalDurations.containsKey(id);
//
//        if (hasCachedNetDuration.test(taskId)) {
//            return cachedTotalDurations.get(taskId);
//        }
//
//        Stream<TaskLink> descendants = this.findDescendantLinks(taskId, filter);
//        LinkedList<TaskLink> descendantStack = new LinkedList<>();
//        descendants.takeWhile(t -> hasCachedNetDuration.test(ID.of(t.child())))
//                .forEachOrdered(descendantStack::push);
//
//        TaskEntity task = this.getTask(taskId);
//        if (descendantStack.isEmpty()) {
//            return this.getTask(taskId).duration();
//        }
//
//        Duration durationSum = cachedTotalDurations.get(ID.of(descendantStack.poll().child()));
//        TaskLink next = descendantStack.poll();
//        while (next != null) {
//            durationSum = task.project()
//                    ? durationSum.plus(next.projectDuration())
//                    : durationSum.plus(next.child().duration());
//            cachedTotalDurations.put(ID.of(next.child()), durationSum);
//            next = descendantStack.poll();
//        }
//
//        cachedTotalDurations.put(taskId, durationSum.plus(task.duration()));
//
//        if (save) {
//            netDurationRepository.save(new NetDuration(
//                    task,
//                    0,
//                    durationSum)
//            );
//        }
//        return durationSum;
//    }

    @Override
    public int getLowestImportanceOfDescendants(TaskID ancestorId) {
        QNetDuration qNetDuration = QNetDuration.netDuration;
        return netDurationRepository.findOne(
                        qNetDuration.task.id.eq(ancestorId.val())).orElseThrow()
                .importance();
    }

    @Override
    public Stream<TaskLink> findLeafTaskLinks(TaskNodeTreeFilter filter) {
        return StreamSupport.stream(linkRepository.findAll(
                Q_LINK.child.childLinks.isEmpty()
                        .and(this.filterLinkPredicate(filter)))
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
