package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.model.data.RoutineStepHierarchy;
import com.trajan.negentropy.model.data.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Transactional
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
public class NetDurationHelper {
    private final EntityQueryService entityQueryService;
    private final NetDurationRepository netDurationRepository;
    private final LinkRepository linkRepository;

    private Map<LinkID, Duration> netDurations = new HashMap<>();
    private MultiValueMap<LinkID, LinkID> projectChildrenOutsideDurationLimitMap = new LinkedMultiValueMap<>();
    private final NonSpecificTaskNodeTreeFilter filter;

    public Map<TaskID, Duration> getNetDurations(Iterable<TaskLink> links) {
        List<TaskEntity> tasks = StreamSupport.stream(links.spliterator(), false)
                .map(TaskLink::child)
                .collect(Collectors.toList());

        List<NetDuration> estimates = netDurationRepository.findByTaskIn(tasks);

        return estimates.stream()
//                .filter(estimate ->
//                        filter == null || (filter.importanceThreshold() == null
//                        || estimate.importance() <= filter.importanceThreshold()))
                .collect(Collectors.toMap(
                        estimate -> ID.of(estimate.task()),
                        NetDuration::val
                ));
    }

    private Duration getNetDurationWithLimit(TaskOrTaskLinkEntity current, RoutineStepHierarchy parent,
                                             Duration durationLimit, boolean customDurationLimit) {
        LinkID currentLinkId = null;
        TaskEntity currentTask;
        if (current instanceof TaskLink link) {
            currentTask = link.child();
            currentLinkId = ID.of(link);
        } else if (current instanceof TaskEntity task) {
            currentTask = task;
        } else {
            throw new RuntimeException("Unknown type of TaskOrTaskLinkEntity");
        }

        Map<TaskLink, Duration> requiredDurations = new HashMap<>();
        List<TaskLink> childLinks = entityQueryService.findChildLinks(ID.of(currentTask), filter).toList();
        for (TaskLink childLink : childLinks) {
            if (childLink.child().required()) {
                requiredDurations.put(childLink, getNetDuration(childLink));
            }
        }

        durationLimit = durationLimit.minus(requiredDurations.values().stream()
                .reduce(Duration.ZERO, Duration::plus));
        Duration currentDurationSum = currentTask.duration();

        boolean full = false;
        RoutineStepEntityHierarchy childHierarchy = parent != null
                ? new RoutineStepEntityHierarchy(currentTask, parent.routine())
                : null;
        for (TaskLink childLink : childLinks) {
            Duration childDuration = requiredDurations.containsKey(childLink)
                    ? requiredDurations.get(childLink)
                    : getNetDuration(childLink);

            Duration potentialDuration = currentDurationSum.plus(childDuration);
            if (!full && durationLimit.compareTo(potentialDuration) >= 0) {
                currentDurationSum = potentialDuration;
                if (parent != null) getNetDuration(childLink, childHierarchy, null);
            } else if (childLink.child().required() && (parent != null)) {
                getNetDuration(childLink, childHierarchy, null);
            } else {
                log.trace("Omitting " + childLink.child().name() + " from hierarchy");
                if (currentLinkId != null && !customDurationLimit) {
                    projectChildrenOutsideDurationLimitMap.add(currentLinkId, ID.of(childLink));
                }
                full = true;
            }
        }

        if (parent != null) parent.addToHierarchy(childHierarchy);

        return currentDurationSum;
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

    public Duration getNetDuration(TaskLink current, RoutineStepHierarchy parent, Duration durationLimit) {
        Duration realDurationLimit = null;
        boolean customDurationLimit = false;
        if (durationLimit != null) {
            realDurationLimit = durationLimit;
            customDurationLimit = true;
        } else if (current.child().project() && current.projectDuration() != null) {
            realDurationLimit = current.projectDuration();
        }

        if (realDurationLimit != null) {
            return getNetDurationWithLimit(current, parent, realDurationLimit, customDurationLimit);
        } else {
            return process(current, parent);
        }
    }

    public Duration getNetDuration(TaskEntity current, RoutineStepHierarchy parent, Duration durationLimit) {
        if (durationLimit != null) {
            return getNetDurationWithLimit(current, parent, durationLimit, true);
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
            return netDurations.get(ID.of(current));
        } else {
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
        if (parent!= null) parent.addToHierarchy(child);
        return netDurationResult;
    }
}
