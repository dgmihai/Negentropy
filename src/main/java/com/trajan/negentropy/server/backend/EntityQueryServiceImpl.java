package com.trajan.negentropy.server.backend;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.*;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.QNetDuration;
import com.trajan.negentropy.model.entity.routine.QRoutineEntity;
import com.trajan.negentropy.model.entity.routine.QRoutineStepEntity;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.server.backend.repository.*;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QSort;
import org.springframework.stereotype.Service;

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
                // TODO: Filter by eta limit as well
//                builder.and(Q_LINK.projectEtaLimit.after(filter.availableAtTime() != null
//                        ? filter.availableAtTime().toLocalTime()
//                        : LocalTime.now()));
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

            try {
                // Filter by included task IDs, and if this filter is by inner join or not
                if (filter.includedTagIds() != null && !filter.includedTagIds().isEmpty()) {

                        List<TagEntity> includedTags = filter.includedTagIds()
                                .stream()
                                .map(tagId -> tagRepository.findById(tagId.val()).get())
                                .toList();

                        Consumer<TagEntity> filterFunction =
                                (filter.options().contains(TaskTreeFilter.INNER_JOIN_INCLUDED_TAGS)) ?
                                        tagEntity -> builder.and(qTask.tags.contains(tagEntity)) :
                                        tagEntity -> builder.or(qTask.tags.contains(tagEntity));

                        includedTags.forEach(filterFunction);
                }

                // Filter by tags that must be excluded
                if (filter.excludedTagIds() != null && !filter.excludedTagIds().isEmpty()) {
                        List<TagEntity> excludedTags = filter.excludedTagIds()
                                .stream()
                                .map(tagId -> tagRepository.findById(tagId.val()).get())
                                .toList();

                        excludedTags.forEach(tagEntity -> builder.and(qTask.tags.contains(tagEntity).not()));
                }
            } catch (NoSuchElementException e) {
                    logger.warn("Filter contained invalid tag ID: " + e.getMessage());
                    throw e;
            }

            if (filter.hasChildren() != null) {
                if (filter.hasChildren()) {
                    builder.and(qTask.childLinks.isNotEmpty());
                } else {
                    builder.and(qTask.childLinks.isEmpty());
                }
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
    public Stream<TaskLink> findLinksNested(TaskNodeTreeFilter filter) {
        HierarchicalSearchHelper helper = new HierarchicalSearchHelper();
        return helper.search(filter, null);
    }

    public class HierarchicalSearchHelper {
        private final Set<LinkID> visited = new HashSet<>();
        private final Stream.Builder<TaskLink> streamBuilder = Stream.builder();
        private Consumer<TaskLink> consumer;
        private TaskNodeTreeFilter filter;

        private void process(TaskLink link) {
            if (consumer != null) consumer.accept(link);
            visited.add(ID.of(link));
            streamBuilder.add(link);
        }

        public synchronized Stream<TaskLink> search(TaskNodeTreeFilter filter, Consumer<TaskLink> consumer) {
            this.filter = filter;
            this.consumer = consumer;

            List<TaskLink> results = findLinks(filter).toList();

            results.forEach(this::process);
            results.forEach(link -> this.recurse(link.parentId()));

            return streamBuilder.build();
        }

        private void recurse(TaskID currentTaskId) {
            findParentLinks(currentTaskId, null)
                    .filter(link -> !visited.contains(ID.of(link)))
                    .peek(this::process)
                    .map(link -> ID.of(link.parent()))
                    .filter(Objects::nonNull)
                    .toList()
                    .forEach(this::recurse);
        }
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

    @Override
    public boolean matchesFilter(TaskID taskId, TaskNodeTreeFilter filter) {
        return linkRepository.exists(this.filterTaskPredicate(filter, Q_TASK)
                .and(Q_LINK.child.id.eq(taskId.val())));
    }

    @Override
    public boolean matchesFilter(LinkID linkId, TaskNodeTreeFilter filter) {
        return linkRepository.exists(this.filterLinkPredicate(filter)
                .and(Q_LINK.id.eq(linkId.val())));
    }

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

    @Override
    public int getLowestImportanceOfDescendants(TaskID ancestorId) {
        QNetDuration qNetDuration = QNetDuration.netDuration;
        return netDurationRepository.findOne(
                        qNetDuration.task.id.eq(ancestorId.val())).orElseThrow()
                .importance();
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

    @Autowired private EntityManager entityManager;

    @Override
    public JPAQuery<RoutineStepEntity> findRoutinesContainingLink(LinkID linkId) {
        return fromRoutines()
                .where(QRoutineStepEntity.routineStepEntity.link.id.eq(linkId.val()));
    }

    @Override
    public JPAQuery<RoutineStepEntity> findOnlyReadyRoutinesContainingLink(LinkID linkId) {
        return onlyReadyRoutines()
                .where(QRoutineStepEntity.routineStepEntity.link.id.eq(linkId.val()));
    }

    @Override
    public JPAQuery<RoutineStepEntity> findOnlyReadyRoutinesContainingLinks(Set<LinkID> linkIds) {
        return onlyReadyRoutines()
                .where(QRoutineStepEntity.routineStepEntity.link.id.in(linkIds.stream()
                        .map(ID::val)
                        .toList()));
    }

    @Override
    public JPAQuery<RoutineStepEntity> findOnlyReadyRoutinesContainingTask(TaskID taskId) {
        return onlyReadyRoutines()
                .where(QRoutineStepEntity.routineStepEntity.task.id.eq(taskId.val()));
    }

    @Override
    public JPAQuery<RoutineStepEntity> findOnlyReadyRoutinesContainingTasks(Set<TaskID> taskIds) {
        return onlyReadyRoutines()
                .where(QRoutineStepEntity.routineStepEntity.task.id.in(taskIds.stream()
                        .map(ID::val)
                        .toList()));
    }

    private BooleanBuilder fromReadyRoutines(boolean onlyReady) {
        QRoutineEntity routine = QRoutineEntity.routineEntity;
        BooleanBuilder conditions = new BooleanBuilder()
                .and(routine.autoSync.isTrue());

        if (onlyReady) {
            conditions.andAnyOf(routine.status.eq(TimeableStatus.ACTIVE), routine.status.eq(TimeableStatus.NOT_STARTED));
        }

        return conditions;
    }

    private JPAQuery<RoutineStepEntity> onlyReadyRoutines() {
        return fromRoutines()
                .where(fromReadyRoutines(true));
    }

    private JPAQuery<RoutineStepEntity> fromRoutines() {
        JPAQuery<RoutineStepEntity> query = new JPAQuery<>(entityManager);
        QRoutineStepEntity routineStep = QRoutineStepEntity.routineStepEntity;
        QRoutineEntity routine = QRoutineEntity.routineEntity;

        return query.select(routineStep)
                .from(routineStep)
                .join(routineStep.routine, routine)
                .where(fromReadyRoutines(true));
    }
}
