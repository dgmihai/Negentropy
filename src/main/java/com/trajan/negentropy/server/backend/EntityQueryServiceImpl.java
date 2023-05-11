package com.trajan.negentropy.server.backend;

import com.querydsl.core.BooleanBuilder;
import com.trajan.negentropy.server.backend.entity.*;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.backend.repository.TimeEstimateRepository;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TagID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service("entityQueryService")
@Transactional
public class EntityQueryServiceImpl implements EntityQueryService {
    private static final Logger logger = LoggerFactory.getLogger(EntityQueryServiceImpl.class);

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private TimeEstimateRepository timeEstimateRepository;
    @Autowired private TagRepository tagRepository;
    
    private static final QTaskLink Q_LINK = QTaskLink.taskLink;
    private static final QTaskEntity Q_TASK = QTaskEntity.taskEntity;

    private Map<TaskID, Duration> cachedNetDurations = new HashMap<>();
    private TaskFilter activeFilter = new TaskFilter();
//
//    @PostConstruct
//    public void init() {
//        queryFactory = new JPAQueryFactory(em);
//    }

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

    private BooleanBuilder filterLink(TaskFilter filter) {
        BooleanBuilder builder = new BooleanBuilder();
        if (filter != null) {
            if (filter.importanceThreshold() != null) {
                builder.and(Q_LINK.importance.loe(filter.importanceThreshold()));
            }
        }
        return new BooleanBuilder();
    }

    private BooleanBuilder filterTask(TaskFilter filter, QTaskEntity qTask) {
        BooleanBuilder builder = new BooleanBuilder();
        if (filter != null) {
            if (!filter.name().isBlank()) {
                builder.and(qTask.name.lower().contains(filter.name().toLowerCase()));
            }
            if (!filter.includedTagIds().isEmpty()) {
                filter.includedTagIds().stream()
                        .map(this::getTag)
                        .forEach(tagEntity -> builder.and(qTask.tags.contains(tagEntity)));
            }
            if (!filter.excludedTagIds().isEmpty()) {
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
        logger.debug("findTasks");
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
        logger.debug("findChildCount");
        return (int) linkRepository.count(byParent(parentId, filter));
    }

    @Override
    public boolean hasChildren(TaskID parentId, TaskFilter filter) {
        logger.debug("hasChildren");
        return linkRepository.exists(byParent(parentId, filter));
    }

    @Override
    public Stream<TaskLink> findChildLinks(TaskID parentId, TaskFilter filter) {
        logger.debug("findChildLinks");
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(byParent(parentId, filter), sort)
                .spliterator(), false);
  }

    @Override
    public Stream<TaskEntity> findChildTasks(TaskID parentId, TaskFilter filter) {
        logger.debug("findChildTasks");
        return this.findChildLinks(parentId, filter).map(TaskLink::child);
    }

    @Override
    public Stream<TaskLink> findParentLinks(TaskID childId, TaskFilter filter) {
        logger.debug("findParentLinks");
        QSort sort = new QSort(Q_LINK.position.asc());
        return StreamSupport.stream(linkRepository.findAll(byChild(childId, filter), sort)
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findParentTasks(TaskID childId, TaskFilter filter) {
        logger.debug("findParentTasks");
        return this.findParentLinks(childId, filter).map(TaskLink::parent);
    }

    @Override
    public Stream<TaskLink> findAncestorLinks(TaskID descendantId, TaskFilter filter) {
        logger.debug("findAncestorLinks");
        return dfs(descendantId, filter,
                this::findParentLinks,
                link -> ID.of(link.parent()))
                .stream();
    }

    @Override
    public Stream<TaskEntity> findAncestorTasks(TaskID descendantId, TaskFilter filter) {
        logger.debug("findAncestorTasks");
        return this.findAncestorLinks(descendantId, filter).map(TaskLink::parent);
    }

    @Override
    public Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskFilter filter) {
        logger.debug("findDescendantLinks");
        return dfs(ancestorId, filter,
                this::findChildLinks,
                link -> ID.of(link.child()))
                .stream();
    }

    @Override
    public Stream<TaskEntity> findDescendantTasks(TaskID ancestorId, TaskFilter filter) {
        logger.debug("findDescendantTasks");
        return this.findDescendantLinks(ancestorId, filter).map(TaskLink::child);
    }

    private List<TaskLink> dfs (TaskID rootId, TaskFilter filter,
                                 BiFunction<TaskID, TaskFilter, Stream<TaskLink>> getNextLinks,
                                 Function<TaskLink, TaskID> getNextLink) {
        List<TaskLink> results = new ArrayList<>();
        this.dfsRecursive(rootId, results, filter, getNextLinks, getNextLink, true);
        return results;
    }

    private void dfsRecursive(TaskID id, List<TaskLink> results, TaskFilter filter,
                              BiFunction<TaskID, TaskFilter, Stream<TaskLink>> getNextLinks,
                              Function<TaskLink, TaskID> getNextLink,
                              boolean first) {
        if (id != null || first) {
            getNextLinks.apply(id, filter)
                    .peek(results::add)
                    .forEachOrdered(link -> dfsRecursive(getNextLink.apply(link), results, filter,
                            getNextLinks, getNextLink, false));
        }
    }

    @Override
    public TimeEstimate getTimeEstimate(TaskID taskId) {
        logger.debug("getTimeEstimate");
        // TODO: Implement importance in time estimate caching
        return this.getTimeEstimate(taskId, 0);
    }

    @Override
    public TimeEstimate getTimeEstimate(TaskID taskId, int importance) {
        logger.debug("getTimeEstimate");
        TaskEntity task = this.getTask(taskId);
        return task.timeEstimates().stream()
                .filter(
                timeEstimate -> importance == timeEstimate.importance())
                .findFirst()
                .orElseThrow();
    }

    @Override
    public Stream<TimeEstimate> getTimeEstimatesWithImportanceThreshold(TaskID taskId, int importanceDifference) {
        logger.debug("getTimeEstimatesWithImportanceThreshold");
        TaskEntity task = this.getTask(taskId);
        int lowestImportance = this.getLowestImportanceOfDescendants(taskId);
        return task.timeEstimates().stream()
                .filter(timeEstimate -> {
                    int difference = lowestImportance - importanceDifference;
                    return timeEstimate.importance() <= difference;
                });
    }

    @Override
    public Duration calculateNetDuration(TaskID taskId, TaskFilter filter) {
        logger.debug("calculateNetDuration");
        if (filter.name().isBlank() && filter.includedTagIds().isEmpty() && filter.excludedTagIds().isEmpty()) {
            return getTimeEstimate(taskId, filter.importanceThreshold()).netDuration();
        }

        boolean filterCached = filter.equals(activeFilter);

        if (!filterCached) {
            cachedNetDurations.clear();
        }

        Predicate<TaskID> hasCachedNetDuration =
                id -> filterCached && cachedNetDurations.containsKey(id);

        if (hasCachedNetDuration.test(taskId)) {
            return cachedNetDurations.get(taskId);
        }

        Stream<TaskEntity> descendants = this.findDescendantTasks(taskId, filter);
        LinkedList<TaskEntity> descendantStack = new LinkedList<>();
        descendants.takeWhile(t -> hasCachedNetDuration.test(ID.of(t)))
                .forEachOrdered(descendantStack::push);

        TaskEntity task = this.getTask(taskId);
        if (descendantStack.isEmpty()) {
            return this.getTask(taskId).duration();
        }

        Duration durationSum = cachedNetDurations.get(ID.of(descendantStack.poll()));
        TaskEntity next = descendantStack.poll();
        while (next != null) {
            durationSum = durationSum.plus(next.duration());
            cachedNetDurations.put(ID.of(next), durationSum);
            next = descendantStack.poll();
        }

        cachedNetDurations.put(taskId, durationSum.plus(task.duration()));
        return durationSum;
    }

    @Override
    public int getLowestImportanceOfDescendants(TaskID ancestorId) {
        logger.debug("getLowestImportanceOfDescendants");
        QTimeEstimate qTimeEstimate = QTimeEstimate.timeEstimate;
        QSort sort = new QSort(qTimeEstimate.importance.desc());
        return timeEstimateRepository.findOne(
                qTimeEstimate.task.id.eq(ancestorId.val())).orElseThrow()
                .importance();
    }

    @Override
    public Stream<TaskLink> findLeafTaskLinks(TaskFilter filter) {
        logger.debug("findLeafTaskLinks");
        return StreamSupport.stream(linkRepository.findAll(
                Q_LINK.child.childLinks.isEmpty()
                        .and(this.filterLink(filter)))
                .spliterator(), false);
    }

    @Override
    public Stream<TaskEntity> findOrphanedTasks() {
        logger.debug("findOrphanedTasks");
        QTaskEntity qTask = QTaskEntity.taskEntity;
        return StreamSupport.stream(taskRepository.findAll(
                qTask.parentLinks.isEmpty())
                .spliterator(), false);
    }
}
