package com.trajan.negentropy.server.backend.netduration;

import com.google.common.base.Stopwatch;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.TaskOrTaskLinkEntity;
import com.trajan.negentropy.server.backend.netduration.RefreshHierarchy.RoutineRefreshHierarchy;
import com.trajan.negentropy.server.backend.netduration.RefreshHierarchy.StepRefreshHierarchy;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.*;

import static com.trajan.negentropy.server.backend.netduration.RefreshHierarchy.matchesData;

@Component
@Scope("prototype")
@Slf4j
@Getter
@Setter
@Benchmark(millisFloor = 10)
public class LinkHierarchyIterator {
    private NetDurationHelper netDurationHelper;
    private Map<LinkID, Duration> netDurations = new HashMap<>();
    private MultiValueMap<LinkID, LinkID> projectChildrenOutsideDurationLimitMap = new LinkedMultiValueMap<>();

    Duration process(TaskLink current, RoutineStepHierarchy parent) {
        Duration leafDuration = current.child().duration();
        RoutineStepEntityHierarchy child = null;

        if (parent != null) {
            if (parent instanceof RoutineRefreshHierarchy) {
                log.debug("Getting remaining duration for <" + current.child().name() + ">");
                RoutineService routineService = SpringContext.getBean(RoutineService.class);
                try {
                    leafDuration = TimeableUtil.get().getRemainingDuration(parent.routine().children()
                                    .stream()
                                    .filter(step -> matchesData(step, current))
                                    .findFirst()
                                    .orElseThrow(),
                            routineService.now());
                } catch (NoSuchElementException e) {
                    log.error("Could not find routine step for task <" + current.child().name() + "> in routine <" + parent.routine().id()
                            + ">", e);
                    leafDuration = current.child().duration();
                }
            } else if (parent instanceof StepRefreshHierarchy refreshHierarchy) {
                leafDuration = refreshHierarchy.getRemainingDuration(current);
            }
            child = RoutineStepEntityHierarchy.create(current, parent.routine(), parent);
            if (parent.exceedsLimit()) {
                child.setExceedsLimit();
            }
        }

        if (current.id() != null && netDurations.containsKey(ID.of(current)) && parent == null) {
            log.trace("Returning cached net duration of " + current.child().name() + ": " + netDurations.get(ID.of(current)));
            return netDurations.get(ID.of(current));
        } else {
            log.trace("Returning iterative net duration of " + current.child().name() + ": " + netDurations.get(ID.of(current)));
            return iterate(leafDuration, parent, child, ID.of(current.child()));
        }
    }

    Duration process(TaskEntity current, RoutineStepHierarchy parent) {
        Duration leafDuration = current.duration();
        RoutineStepEntityHierarchy child = null;

        if (parent != null) {
            if (parent instanceof RoutineRefreshHierarchy) {
                RoutineService routineService = SpringContext.getBean(RoutineService.class);
                try {
                    leafDuration = TimeableUtil.get().getRemainingDurationIncludingLimitExceeded(parent.routine().children()
                                    .stream()
                                    .filter(step -> step.task().id().equals(current.id()))
                                    .findFirst()
                                    .orElseThrow(),
                            routineService.now());
                } catch (NoSuchElementException e) {
                    log.error("Could not find routine step for task <" + current.name() + "> in routine <" + parent.routine().id()
                            + ">", e);
                }
            } else if (parent instanceof StepRefreshHierarchy refreshHierarchy) {
                leafDuration = refreshHierarchy.getRemainingDuration(current);
            }
            child = RoutineStepEntityHierarchy.create(current, parent.routine(), parent);
        }

        return iterate(leafDuration, parent, child, ID.of(current));
    }

    Duration iterate(Duration durationSum, RoutineStepHierarchy parent, RoutineStepHierarchy child,
                             TaskID parentId) {
        for (TaskLink link : this.findChildLinks(parentId)) {
            Duration result = netDurationHelper.inner_calculateHierarchicalNetDuration(link, child, null);
            if (result != null) {
                netDurations.put(ID.of(link), result);
                durationSum = durationSum.plus(result);
            }
        }
        if (parent != null) {
            if (child instanceof RoutineStepEntityHierarchy stepHierarchy) {
                parent.addToHierarchy(stepHierarchy);
            }
        }
        return durationSum;
    }

    void iterateAsExceededLimit(TaskOrTaskLinkEntity entity, @NotNull RoutineStepHierarchy parent, boolean customLimit) {
        RoutineStepEntityHierarchy current = RoutineStepEntityHierarchy.create(entity, parent.routine(), parent);
        current.setExceedsLimit();

        LinkID currentLinkId = (entity instanceof TaskLink link)
                ? ID.of((link))
                : null;

        if (currentLinkId != null && !customLimit && parent instanceof RoutineStepEntityHierarchy parentStepHierarchy) {
//                && !(parent instanceof RefreshHierarchy)) {
            parentStepHierarchy.step.link().ifPresent(parentId ->
                    projectChildrenOutsideDurationLimitMap.add(ID.of(parentId), currentLinkId));
        }

        TaskID currentTaskId = (entity instanceof TaskEntity task)
                ? ID.of(task)
                : ID.of(((TaskLink) entity).child());

        for (TaskLink child : this.findChildLinks(currentTaskId)) {
            iterateAsExceededLimit(child, current, customLimit);
        }

        parent.addToHierarchy(current);
    }

    Duration iterateWithLimit(TaskOrTaskLinkEntity current, RoutineStepHierarchy parent,
                                             RoutineLimiter limit) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.debug("Calculating net duration with limit of <" + current.name() + ">: " + limit);
        LinkID currentLinkId = null;
        TaskEntity currentTask;

        if (current instanceof TaskLink link) {
            currentTask = link.child();
            currentLinkId = ID.of(link);
            if (netDurations.containsKey(currentLinkId) && parent == null && !limit.customLimit()) {
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

        if (!limit.customLimit() && currentLinkId != null) projectChildrenOutsideDurationLimitMap.remove(currentLinkId);

        List<TaskLink> childLinks = this.findChildLinks(ID.of(currentTask));

        Map<TaskLink, Duration> requiredDurations = childLinks.stream()
                .filter(l -> l.child().required())
                .peek(l -> log.trace("Calculating required duration for " + l.child().name()))
                .map(l -> netDurationHelper.inner_calculateHierarchicalNetDuration(l, null, null))
                .collect(HashMap::new, (m, v) -> m.put(childLinks.get(m.size()), v), HashMap::putAll);

        log.trace("Required durations: " + requiredDurations);
        log.trace("Current task duration: " + currentTask.duration());
        limit.durationSum(currentTask.duration());
        limit.include(requiredDurations.values(), true);
        log.trace("Duration sum: " + limit.durationSum() + " with " + limit.count() + " children");

        RoutineStepEntityHierarchy currentHierarchy = null;
        if (parent != null) {
            currentHierarchy = RoutineStepEntityHierarchy.create(current, parent.routine(), parent);
        }

        for (TaskLink childLink : childLinks) {
            log.trace("Processing <" + childLink.child().name() + ">");

            if (childLink.child().required()) {
                log.debug("<" + childLink.child().name() + "> is required");
                netDurationHelper.inner_calculateHierarchicalNetDuration(childLink, currentHierarchy, null);
            } else if (limit.exceeded()) {
                if (currentLinkId != null && !limit.customLimit()) {
                    projectChildrenOutsideDurationLimitMap.add(currentLinkId, ID.of(childLink));
                }
                if (currentHierarchy != null) {
                    iterateAsExceededLimit(childLink, currentHierarchy, limit.customLimit());
                }
            } else {
                Duration childDuration = netDurationHelper.inner_calculateHierarchicalNetDuration(childLink, null, null);
                log.trace("Calculated net duration of <" + childLink.child().name() + ">: " + childDuration);
                if (limit.wouldExceed(childDuration)) {
                    log.debug("<" + childLink.child().name() + "> exceeds limit");
                    limit.exceeded(true);
                    if (currentLinkId != null && !limit.customLimit()) {
                        projectChildrenOutsideDurationLimitMap.add(currentLinkId, ID.of(childLink));
                    }
                    if (currentHierarchy != null) {
                        iterateAsExceededLimit(childLink, currentHierarchy, limit.customLimit());
                    }
                } else {
                    log.debug("<" + childLink.child().name() + "> does NOT exceed duration");
                    childDuration = netDurationHelper.inner_calculateHierarchicalNetDuration(
                            childLink,
                            currentHierarchy,
                            null);
                    log.trace("Second calculated net duration of <" + childLink.child().name() + ">: " + childDuration);
                    limit.include(childDuration, false);
                }
            }
        }

        if (parent != null) parent.addToHierarchy(currentHierarchy);
        log.trace("Returning limit calculated net duration of <" + currentTask.name() + "> : " + limit.durationSum());
        log.debug("Calculated net duration of <" + currentTask.name() + "> in " + stopwatch.stop().elapsed().toMillis() + "ms");
        return limit.durationSum();
    }

    private List<TaskLink> findChildLinks(TaskID childId) {
        return netDurationHelper.adjacencyMap().get(Objects.requireNonNullElse(childId, TaskID.nil()));
    }
}