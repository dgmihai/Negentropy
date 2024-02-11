package com.trajan.negentropy.server.backend.netduration;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.HasTaskLinkOrTaskEntity;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.RoutineServiceImpl.LimitedDataWrapper;
import com.trajan.negentropy.util.SpringContext;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Transactional
@Getter
@Setter
@Slf4j
@Benchmark(millisFloor = 10)
@Component
@Scope("prototype")
public class NetDurationHelper {
    @Autowired private QueryService queryService;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationRepository netDurationRepository;
    @Autowired private LinkRepository linkRepository;

    private ListMultimap<TaskID, TaskLink> adjacencyMap;
    private NonSpecificTaskNodeTreeFilter filter;
    private SyncID syncId = null;

    @Autowired private LinkHierarchyIterator linkHierarchyIterator;

    @PostConstruct
    public void init() {
        linkHierarchyIterator.netDurationHelper(this);
        loadAdjacencyMap();
    }

    public synchronized Map<TaskID, Duration> getAllNetTaskDurations() {
        this.loadAdjacencyMap();
        return entityQueryService.findTasks(filter)
                .collect(Collectors.toMap(
                        ID::of,
                        task -> this.inner_calculateHierarchicalNetDuration(task, null, null)));
    }

    public synchronized Map<LinkID, Duration> getAllNetNodeDurations() {
        this.loadAdjacencyMap();
        return adjacencyMap.values().stream()
                .collect(Collectors.toMap(
                        ID::of,
                        link -> this.inner_calculateHierarchicalNetDuration(link, null, null)));
    }

    public synchronized Duration getNetDuration(HasTaskLinkOrTaskEntity current) {
        this.loadAdjacencyMap();
        return inner_calculateHierarchicalNetDuration(current, null, null);
    }

    public synchronized Duration getNetDuration(TaskLink current) {
        this.loadAdjacencyMap();
        return inner_calculateHierarchicalNetDuration(current, null, null);
    }

    public synchronized Duration calculateHierarchicalNetDuration(HasTaskLinkOrTaskEntity current, RoutineStepHierarchy parent, RoutineLimiter limit) {
        loadAdjacencyMap();
        return inner_calculateHierarchicalNetDuration(current, parent, limit);
    }

    Duration inner_calculateHierarchicalNetDuration(HasTaskLinkOrTaskEntity current, RoutineStepHierarchy parent, RoutineLimiter limit) {
        if (current.link().isPresent()) {
            TaskLink link = current.link().get();
            if (limit == null) {
                limit = new RoutineLimiter(null, null, null, false);
            }

            if (current.task().project() && !limit.customLimit()) {
                LocalDateTime etaLimit = null;

                if (link.projectEtaLimit().isPresent()) {
                    LocalDateTime time = !(parent instanceof RefreshHierarchy)
                            ? SpringContext.getBean(RoutineService.class).now()
                            : parent.routine().creationTimestamp();
                    etaLimit = link.projectEtaLimit().get().atDate(time.toLocalDate());
                }

                limit = new RoutineLimiter(
                        link.projectDurationLimit().orElse(null),
                        link.projectStepCountLimit().orElse(null),
                        etaLimit,
                        limit.customLimit());
            }

            return (!limit.isEmpty())
                    ? linkHierarchyIterator.iterateWithLimit(current, parent, limit)
                    : linkHierarchyIterator.process(current, parent);
        } else {
            if (limit != null && !limit.isEmpty() && (current.task().project() || limit.exceeded())) {
                return linkHierarchyIterator.iterateWithLimit(current, parent, limit);
            } else {
                return linkHierarchyIterator.process(current, parent);
            }
        }
    }

    public synchronized Duration calculateHierarchicalNetDuration(LimitedDataWrapper current, RoutineStepHierarchy parent, RoutineLimiter limit) {
        loadAdjacencyMap();
        return inner_calculateHierarchicalNetDuration(current, parent, limit);
    }

    void loadAdjacencyMap() {
            adjacencyMap = Multimaps.index(
                    entityQueryService.findLinksGroupedHierarchically(filter).toList(),
                    link -> Objects.requireNonNullElse(link.parentId(), TaskID.nil()));
    }

    private Duration inner_calculateHierarchicalNetDuration(LimitedDataWrapper current, RoutineStepHierarchy parent, RoutineLimiter limit) {
        Integer stepCountLimit = current.stepCountLimit();
        if (limit != null) {
            limit = new RoutineLimiter(limit.durationLimit(), stepCountLimit, limit.etaLimit(), true);
        } else {
            limit = new RoutineLimiter(null, stepCountLimit, null, true);
        }

        if (current.data() instanceof TaskEntity task) {
            return inner_calculateHierarchicalNetDuration(task, parent, limit);
        } else if (current.data() instanceof TaskLink link) {
            return inner_calculateHierarchicalNetDuration(link, parent, limit);
        } else {
            throw new RuntimeException("Unknown type of LimitedData");
        }
    }
}
