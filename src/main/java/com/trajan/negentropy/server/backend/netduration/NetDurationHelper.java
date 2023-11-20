package com.trajan.negentropy.server.backend.netduration;

import com.google.common.base.Stopwatch;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.data.RoutineStepHierarchy;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.TaskOrTaskLinkEntity;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
@Benchmark(trace = true)
@Component
@Scope("prototype")
public class NetDurationHelper {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationRepository netDurationRepository;
    @Autowired private LinkRepository linkRepository;

    private Map<LinkID, Duration> netDurations = new HashMap<>();
    private MultiValueMap<LinkID, LinkID> projectChildrenOutsideDurationLimitMap = new LinkedMultiValueMap<>();
    private NonSpecificTaskNodeTreeFilter filter;

    public Map<TaskID, Duration> getAllNetTaskDurations() {
        return entityQueryService.findTasks(filter)
                .collect(Collectors.toMap(
                        ID::of,
                        this::getNetDuration));
    }

    public Map<LinkID, Duration> getAllNetNodeDurations() {
        return entityQueryService.findLinks(filter)
                .collect(Collectors.toMap(
                        ID::of,
                        this::getNetDuration));
    }

    @RequiredArgsConstructor
    public static class RoutineLimiter {
        private final Duration durationLimit;
        private Duration durationSum = Duration.ZERO;
        private final Integer countLimit;
        private Integer count = 0;
        private final LocalDateTime etaLimit;
        private final boolean customLimit;
        private boolean exceeded;

        public boolean isEmpty() {
            return durationLimit == null && countLimit == null && etaLimit == null;
        }

        public boolean wouldExceed(Duration duration) {
            if (exceeded) return true;

            log.trace("Potential shift: " + duration);
            Duration potential = durationSum.plus(duration);
            log.trace("Potential duration sum: " + potential);
            log.trace("Duration limit: " + durationLimit);

            if ((durationLimit != null && !durationLimit.equals(TaskLink.DEFAULT_PROJECT_DURATION_LIMIT) && durationLimit.compareTo(potential) < 0)
                    || (countLimit != null && !countLimit.equals(TaskLink.DEFAULT_PROJECT_STEP_COUNT_LIMIT) && countLimit <= count + 1)) {
                return true;
            }

            boolean wouldExceed = etaExceedsLimit(duration);
            log.trace("Would exceed: " + wouldExceed);
            return wouldExceed;
        }

        public void include (Duration duration) {
            include(List.of(duration));
        }

        public void include (Collection<Duration> durations) {
            Duration shift = durations.stream().reduce(Duration.ZERO, Duration::plus);
            log.trace("Shift: " + shift);
            durationSum = durationSum.plus(shift);
            log.trace("Duration sum: " + durationSum);
            log.trace("Duration limit: " + durationLimit);
            count += durations.size();

            if ((durationLimit != null && !durationLimit.equals(TaskLink.DEFAULT_PROJECT_DURATION_LIMIT) && durationLimit.compareTo(durationSum) < 0)
                    || (countLimit != null && !countLimit.equals(TaskLink.DEFAULT_PROJECT_STEP_COUNT_LIMIT) && countLimit <= count)) {
                exceeded = true;
            } else {
                exceeded = etaExceedsLimit();
            }

            log.trace("Exceeded: " + exceeded);
        }

        private boolean etaExceedsLimit() {
            return etaExceedsLimit(Duration.ZERO);
        }

        private boolean etaExceedsLimit(Duration duration) {
            log.trace("Checking eta limit: " + etaLimit);
            if (etaLimit != null && !etaLimit.toLocalTime().equals(TaskLink.DEFAULT_PROJECT_ETA_LIMIT)) {
                LocalDateTime potentialEta = LocalDateTime.now().plus(durationSum).plus(duration);
                return potentialEta.toLocalTime().isAfter(etaLimit.toLocalTime());
            } else {
                return false;
            }
        }
    }

    private Duration getNetDurationWithLimit(TaskOrTaskLinkEntity current, RoutineStepHierarchy parent,
                                             RoutineLimiter limit) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        LinkID currentLinkId = null;
        TaskEntity currentTask;
        if (current instanceof TaskLink link) {
            currentTask = link.child();
            currentLinkId = ID.of(link);
            if (netDurations.containsKey(currentLinkId) && parent == null && !limit.customLimit) {
                log.trace("Returning cached net duration");
                Duration result = netDurations.get(currentLinkId);
                if (result != null) return netDurations.get(currentLinkId);
                log.trace("Cached net duration is null, recalculating");
            }
        } else if (current instanceof TaskEntity task) {
            currentTask = task;
        } else {
            throw new RuntimeException("Unknown type of TaskOrTaskLinkEntity");
        }

        if (!limit.customLimit) projectChildrenOutsideDurationLimitMap.remove(currentLinkId);

        Map<TaskLink, Duration> requiredDurations = new HashMap<>();
        List<TaskLink> childLinks = entityQueryService.findChildLinks(ID.of(currentTask), filter).toList();
        for (TaskLink childLink : childLinks) {
            if (childLink.child().required()) {
                requiredDurations.put(childLink, getNetDuration(childLink));
            }
        }

        limit.durationSum = currentTask.duration();
        limit.include(requiredDurations.values());

        RoutineStepEntityHierarchy childHierarchy = parent != null
                ? new RoutineStepEntityHierarchy(currentTask, parent.routine())
                : null;

        for (TaskLink childLink : childLinks) {
            Duration childDuration = requiredDurations.containsKey(childLink)
                    ? requiredDurations.get(childLink)
                    : getNetDuration(childLink);

            if (!limit.exceeded && !limit.wouldExceed(childDuration) && !childLink.child().required()) {
                limit.include(childDuration);
                if (parent != null) getNetDuration(childLink, childHierarchy, null);
            } else if (childLink.child().required() && (parent != null)) {
                getNetDuration(childLink, childHierarchy, null);
            } else if (!childLink.child().required()) {
                log.trace("Omitting " + childLink.child().name() + " from hierarchy");
                if (currentLinkId != null && !limit.customLimit) {
                    projectChildrenOutsideDurationLimitMap.add(currentLinkId, ID.of(childLink));
                }
                // Remove this, and it will add in later steps that fit in the duration instead of stopping at the first
                // one that doesn't.
                limit.exceeded = true;
            }
        }

        if (parent != null) parent.addToHierarchy(childHierarchy);

        log.trace("Returning limit calculated net duration of " + currentTask.name() + ": " + limit.durationSum);
        log.debug("Calculated net duration of " + currentTask.name() + " in " + stopwatch.stop().elapsed().toMillis() + "ms");
        return limit.durationSum;
    }

    public Duration getNetDuration(TaskEntity current) {
        return getNetDuration(current, null, null);
    }

    public Duration getNetDuration(TaskLink current) {
        return getNetDuration(current, null, null);
    }

    public Duration getNetDuration(RoutineStepEntity routineStepEntity) {
        return (routineStepEntity.link().isPresent())
                ? getNetDuration(routineStepEntity.link().get())
                : getNetDuration(routineStepEntity.task());
    }

    public Duration getNetDuration(TaskLink current, RoutineStepHierarchy parent, RoutineLimiter limit) {
        if (limit == null) {
            limit = new RoutineLimiter(null, null, null, false);
        }

        if (!limit.customLimit && current.child().project() && !current.projectDurationLimit().isZero()) {
            limit = new RoutineLimiter(current.projectDurationLimit(), limit.countLimit, limit.etaLimit, limit.customLimit);
        }

        if (!limit.isEmpty() && current.child().project()) {
            return getNetDurationWithLimit(current, parent, limit);
        } else {
            return process(current, parent);
        }
    }

    public Duration getNetDuration(TaskEntity current, RoutineStepHierarchy parent, RoutineLimiter limit) {
        if (limit != null && !limit.isEmpty() && current.project()) {
            return getNetDurationWithLimit(current, parent, limit);
        } else {
            return process(current, parent);
        }
    }

    private Duration process(TaskLink current, RoutineStepHierarchy parent) {
        Duration netDurationResult = current.child().duration();
        RoutineStepEntityHierarchy child = parent != null
                ? new RoutineStepEntityHierarchy(current, parent.routine())
                : null;

        if (current.id() != null && netDurations.containsKey(ID.of(current)) && parent == null) {
            log.trace("Returning cached net duration of " + current.child().name() + ": " + netDurations.get(ID.of(current)));
            return netDurations.get(ID.of(current));
        } else {
            log.trace("Returning iterative net duration of " + current.child().name() + ": " + netDurations.get(ID.of(current)));
            return iterate(netDurationResult, parent, child, ID.of(current.child()));
        }
    }

    private Duration process(TaskEntity current, RoutineStepHierarchy parent) {
        Duration netDurationResult = current.duration();
        RoutineStepEntityHierarchy child = parent != null
                ? new RoutineStepEntityHierarchy(current, parent.routine())
                : null;

        return iterate(netDurationResult, parent, child, ID.of(current));
    }

    private Duration iterate(Duration netDurationResult, RoutineStepHierarchy parent, RoutineStepEntityHierarchy child, TaskID parentId) {
        Stream<TaskLink> childLinkStream = entityQueryService.findChildLinks(parentId, filter);
        for (TaskLink link : childLinkStream.toList()) {
            Duration result = getNetDuration(link, child, null);
            netDurations.put(ID.of(link), result);
            netDurationResult = netDurationResult.plus(result);
        }
        if (parent != null) parent.addToHierarchy(child);
        return netDurationResult;
    }
}
