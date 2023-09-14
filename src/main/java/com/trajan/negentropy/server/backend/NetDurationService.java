package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.NetDurationID;
import com.trajan.negentropy.model.entity.netduration.QNetDuration;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelper;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NetDurationService {

    @Autowired private EntityQueryService entityQueryService;

    @Autowired private NetDurationRepository netDurationRepository;
    @Autowired private NetDurationHelperManager helperManager;
    
    private final Map<TaskTreeFilter, NetDurationHelper> helpers = new HashMap<>();

    public NetDurationHelper getHelper(TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;

        nonNullFilter.name(null); // We don't cache by name

        return helpers.computeIfAbsent(nonNullFilter, f ->
                helperManager.getHelper(nonNullFilter)
        );
    }

    public Map<TaskID, Duration> getAllNetDurations(TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;
        nonNullFilter.name(null); // We don't cache by name

        return entityQueryService.findLinks(nonNullFilter)
                .collect(Collectors.toMap(
                        link -> ID.of(link.child()),
                        link -> this.getNetDuration(link, nonNullFilter)
                ));
    }

    public Map<TaskID, Duration> getAllDescendantsNetDurations(LinkID ancestorId, TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;
        nonNullFilter.name(null); // We don't cache by name

        return entityQueryService.findDescendantLinks(ancestorId, nonNullFilter)
                .collect(Collectors.toMap(
                        link -> ID.of(link.child()),
                        link -> this.getNetDuration(link, nonNullFilter)
                ));
    }

    public NetDuration getNetDurationEntity(TaskID taskId) {
        return netDurationRepository.getReferenceById(new NetDurationID(entityQueryService.getTask(taskId), 0));
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

    public Duration getNetDuration(TaskLink link, TaskNodeTreeFilter filter) {
        TaskNodeTreeFilter nonNullFilter = filter == null ? new TaskNodeTreeFilter() : filter;

        if ((!link.child().project() && link.projectDuration() != null) && (nonNullFilter.isEmpty())) {
            return getNetDurationEntity(ID.of(link.child())).val();
        } else {
            nonNullFilter.name(null); // We don't cache by name

            NetDurationHelper helper = helpers.computeIfAbsent(nonNullFilter, f ->
                    helperManager.getHelper(nonNullFilter)
            );

            return helper.getNetDuration(link);
        }
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
