package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.HasTaskLinkOrTaskEntity;
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

    private Duration getDuration(HasTaskLinkOrTaskEntity entity) {
        return (entity instanceof RoutineStepEntity step)
                ? TimeableUtil.get().getRemainingDuration(
                step, SpringContext.getBean(RoutineService.class).now(), true)
                : entity.duration();
    }

    Duration process(HasTaskLinkOrTaskEntity current, RoutineStepHierarchy parent) {
        Duration leafDuration = this.getDuration(current);
        RoutineStepEntityHierarchy child = null;

        if (parent != null) {
            child = RoutineStepEntityHierarchy.create(current, parent.routine(), parent);
            if (parent.exceedsLimit()) {
                child.setExceedsLimit();
            }
        }

        if (current.link().isPresent()
                && current.link().get().id() != null
                && netDurations.containsKey(ID.of(current.link().get()))
                && parent == null) {
            log.trace("Returning cached net duration of " + current.name() + ": " + netDurations.get(ID.of(current.link().get())));
            return netDurations.get(ID.of(current.link().get()));
        } else {
            log.trace("Returning iterative net duration of <" + current.name() + ">");
            return iterate(leafDuration, parent, child, current);
        }
    }

    Duration iterate(Duration durationSum, RoutineStepHierarchy parent, RoutineStepHierarchy child,
                     HasTaskLinkOrTaskEntity entity) {
        for (HasTaskLinkOrTaskEntity childEntity : this.findChildLinks(entity)) {
            Duration result = netDurationHelper.inner_calculateHierarchicalNetDuration(childEntity, child, null);
            if (result != null) {
                netDurations.put(ID.of(childEntity.link().orElseThrow()), result);
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

    void iterateAsExceededLimit(HasTaskLinkOrTaskEntity entity, @NotNull RoutineStepHierarchy parent, boolean customLimit) {
        RoutineStepEntityHierarchy current = RoutineStepEntityHierarchy.create(entity, parent.routine(), parent);
        current.setExceedsLimit();

        LinkID currentLinkId = (entity instanceof TaskLink link)
                ? ID.of((link))
                : null;

        if (currentLinkId != null && !customLimit && parent instanceof RoutineStepEntityHierarchy parentStepHierarchy) {
            parentStepHierarchy.step.link().ifPresent(parentId ->
                    projectChildrenOutsideDurationLimitMap.add(ID.of(parentId), currentLinkId));
        }

        for (HasTaskLinkOrTaskEntity child : this.findChildLinks(entity)) {
            iterateAsExceededLimit(child, current, customLimit);
        }

        parent.addToHierarchy(current);
    }

    private boolean mustInclude(HasTaskLinkOrTaskEntity entity) {
        if (entity instanceof RoutineStepEntity step) {
            if (Set.of(TimeableStatus.ACTIVE, TimeableStatus.SUSPENDED).contains(step.status())) {
                return true;
            }
        }
        return false;
    }

    Duration iterateWithLimit(HasTaskLinkOrTaskEntity current, RoutineStepHierarchy parent,
                              RoutineLimiter limit) {
        log.trace("Calculating net duration with limit of <" + current.name() + ">: " + limit);
        LinkID currentLinkId = null;

        if (current.link().isPresent()) {
            currentLinkId = ID.of(current.link().get());
            if (netDurations.containsKey(currentLinkId) && parent == null && !limit.customLimit()) {
                log.trace("Returning cached net duration");
                Duration result = netDurations.get(currentLinkId);
                if (result != null) return netDurations.get(currentLinkId);
                log.trace("Cached net duration is null, recalculating");
            }
        }

        if (!limit.customLimit() && currentLinkId != null) projectChildrenOutsideDurationLimitMap.remove(currentLinkId);

        List<HasTaskLinkOrTaskEntity> childLinks = this.findChildLinks(current);

        Map<HasTaskLinkOrTaskEntity, Duration> requiredDurations = childLinks.stream()
                .filter(l -> l.task().required())
                .peek(l -> log.trace("Calculating required duration for " + l.task().name()))
                .map(l -> netDurationHelper.inner_calculateHierarchicalNetDuration(l, null, null))
                .collect(HashMap::new, (m, v) -> m.put(childLinks.get(m.size()), v), HashMap::putAll);

        log.trace("Required durations: " + requiredDurations);
        Duration taskDuration = getDuration(current);
        log.trace("Current task duration: " + taskDuration);
        limit.durationSum(taskDuration);
        limit.include(requiredDurations.values(), true);
        log.trace("Duration sum: " + limit.durationSum() + " with " + limit.count() + " children");

        RoutineStepEntityHierarchy currentHierarchy = null;
        if (parent != null) {
            currentHierarchy = RoutineStepEntityHierarchy.create(current, parent.routine(), parent);
        }

        for (HasTaskLinkOrTaskEntity child : childLinks) {
            log.trace("Processing <" + child.name() + ">");

            if (child.task().required()) {
                log.trace("<" + child.name() + "> is required");
                netDurationHelper.inner_calculateHierarchicalNetDuration(child, currentHierarchy, null);
            } else if (limit.exceeded()) {
                if (currentLinkId != null && !limit.customLimit() && child.link().isPresent()) {
                    projectChildrenOutsideDurationLimitMap.add(currentLinkId, ID.of(child.link().get()));
                }
                if (currentHierarchy != null) {
                    iterateAsExceededLimit(child, currentHierarchy, limit.customLimit());
                }
            } else {
                Duration childDuration = netDurationHelper.inner_calculateHierarchicalNetDuration(child, null, null);
                log.trace("Calculated net duration of <" + child.name() + ">: " + childDuration);
                if (limit.wouldExceed(childDuration) && !mustInclude(child)) {
                    log.trace("<" + child.name() + "> exceeds limit");
                    limit.exceeded(true);
                    if (currentLinkId != null && !limit.customLimit()) {
                        projectChildrenOutsideDurationLimitMap.add(currentLinkId, ID.of(child.link().get()));
                    }
                    if (currentHierarchy != null) {
                        iterateAsExceededLimit(child, currentHierarchy, limit.customLimit());
                    }
                } else {
                    log.trace("<" + child.name() + "> does NOT exceed duration");
                    childDuration = netDurationHelper.inner_calculateHierarchicalNetDuration(
                            child,
                            currentHierarchy,
                            null);
                    limit.include(childDuration, false);
                }
            }
        }

        if (parent != null) parent.addToHierarchy(currentHierarchy);
        log.trace("Returning limit calculated net duration of <" + current.name() + "> : " + limit.durationSum());
        return limit.durationSum();
    }

    private List<HasTaskLinkOrTaskEntity> findChildLinks(HasTaskLinkOrTaskEntity parent) {
        if (parent instanceof RoutineStepEntity step) {
            return step.children().stream()
                    .map(s -> (HasTaskLinkOrTaskEntity) s)
                    .toList();
        }
        TaskID childId = ID.of(parent.task());
        return netDurationHelper.adjacencyMap().get(Objects.requireNonNullElse(childId, TaskID.nil())).stream()
                .map(s -> (HasTaskLinkOrTaskEntity) s)
                .toList();
    }
}