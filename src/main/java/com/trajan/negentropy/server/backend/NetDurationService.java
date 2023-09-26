package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.QNetDuration;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@Benchmark
public class NetDurationService {

    @Autowired private EntityQueryService entityQueryService;

    @Autowired private NetDurationRepository netDurationRepository;
    @Autowired private NetDurationHelperManager helperManager;
    
    private final Map<TaskTreeFilter, NetDurationHelper> helpers = new HashMap<>();

    public synchronized NetDurationHelper getHelper(TaskNodeTreeFilter filter) {
        return helperManager.getHelper(filter);
    }

    public Map<TaskID, Duration> getAllNetTaskDurations(TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;
        nonNullFilter.name(null); // We don't cache by name

        return entityQueryService.findTasks(nonNullFilter)
                .collect(Collectors.toMap(
                        ID::of,
                        link -> this.getNetDuration(link, nonNullFilter)
                ));
    }

    public Map<LinkID, Duration> getAllNetNodeDurations(TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;
        nonNullFilter.name(null); // We don't cache by name

        return entityQueryService.findLinks(nonNullFilter)
                .collect(Collectors.toMap(
                        ID::of,
                        link -> this.getNetDuration(link, nonNullFilter)
                ));
    }

    public void clearLinks(Set<LinkID> durationUpdates) {
        helperManager.clearLinks(durationUpdates);
    }

    @Getter
    @AllArgsConstructor
    public static class NetDurationInfo {
        private Map<TaskID, Duration> netTaskDurations;
        private Map<LinkID, Duration> netNodeDurations;
        private MultiValueMap<LinkID, LinkID> projectChildrenOutsideDurationLimitMap;
        private TaskNodeTreeFilter filter;
    }

    public NetDurationInfo getNetDurationInfo(TaskNodeTreeFilter filter) {
        return new NetDurationInfo(
                getAllNetTaskDurations(filter),
                getAllNetNodeDurations(filter),
                getHelper(filter).projectChildrenOutsideDurationLimitMap(),
                filter);
    }

    public Map<LinkID, Duration> getAllDescendantsNetDurations(LinkID ancestorId, TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;
        nonNullFilter.name(null); // We don't cache by name

        return entityQueryService.findDescendantLinks(ancestorId, nonNullFilter)
                .collect(Collectors.toMap(
                        ID::of,
                        link -> this.getNetDuration(link, nonNullFilter)
                ));
    }

    public NetDuration getNetDurationEntity(TaskID taskId) {
        return netDurationRepository.findByTaskId(taskId.val()).get(0);
    }

    public NetDuration getNetDurationEntity(TaskEntity task) {
        return netDurationRepository.findByTask(task).get(0);
    }

    public Duration getNetDuration(TaskID taskId, TaskNodeTreeFilter filter) {
        return getNetDuration(entityQueryService.getTask(taskId), filter);
    }

    public Duration getNetDuration(TaskEntity task, TaskNodeTreeFilter filter) {
        return entityQueryService.findChildLinks(ID.of(task), filter)
                .map(link -> getNetDuration(link, filter))
                .reduce(task.duration(), Duration::plus);
    }

    public Duration getNetDuration(LinkID linkId, TaskNodeTreeFilter filter) {
        return getNetDuration(entityQueryService.getLink(linkId), filter);
    }

    public synchronized Duration getNetDuration(TaskLink link, TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;
        nonNullFilter.name(null); // We don't cache by name

        // TODO: Retry caching
//        if (!(link.child().project() && link.projectDuration() != null) && (nonNullFilter.isEmpty())) {
//            return getNetDurationEntity(link.child()).val();
//        } else {

            NetDurationHelper helper = helpers.computeIfAbsent(nonNullFilter, f ->
                    helperManager.getHelper(nonNullFilter)
            );

            return helper.getNetDuration(link);
//        }
    }

//    public Stream<NetDuration> getTotalDurationWithImportanceThreshold(TaskID taskId, int importanceDifference) {
//        TaskEntity task = this.getTask(taskId);
//        int lowestImportance = this.getLowestImportanceOfDescendants(taskId);
//        return task.netDurations().stream()
//                .filter(val -> {
//                    int difference = lowestImportance - importanceDifference;
//                    return val.importance() <= difference;
//                });
//    }

    public int getLowestImportanceOfDescendants(TaskID ancestorId) {
        QNetDuration qNetDuration = QNetDuration.netDuration;
        return netDurationRepository.findOne(
                        qNetDuration.task.id.eq(ancestorId.val())).orElseThrow()
                .importance();
    }
}
