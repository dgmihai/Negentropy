package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.NetDurationID;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.LazyInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Transactional
public class NetDurationHelperManager {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationRepository netDurationRepository;
    @Autowired private LinkRepository linkRepository;

    private final Map<NonSpecificTaskNodeTreeFilter, NetDurationHelper> helpers = new HashMap<>();

    public synchronized NetDurationHelper getHelper(TaskNodeTreeFilter filter) {
        return helpers.computeIfAbsent(NonSpecificTaskNodeTreeFilter.from(filter), f ->
            new NetDurationHelper(entityQueryService, netDurationRepository, linkRepository,
                    f)
        );
    }

    public synchronized void clear() {
        helpers.clear();
    }

    public synchronized void clearLinks(Set<LinkID> durationUpdates) {
        helpers.values().forEach(helper ->
                durationUpdates.forEach(linkId -> {
                        helper.netDurations().remove(linkId);
                        helper.projectChildrenOutsideDurationLimitMap().remove(linkId);
                }));
    }

    public synchronized void recalculateTimeEstimates() {
        netDurationRepository.findAll()
                .forEach(estimate -> estimate.val(Duration.ZERO));

        for (TaskEntity task : entityQueryService.findTasks(null).toList()) {
            List<Duration> durations = entityQueryService.findDescendantLinks(ID.of(task), null)
                    .map(link -> link.child().project()
                            ? link.projectDuration()
                            : link.child().duration())
                    .toList();

            Duration sum = task.duration();
            for (Duration duration : durations) {
                if (duration != null) {
                    sum = sum.plus(duration);
                }
            }

            try {
                netDurationRepository.getReferenceById(new NetDurationID(task, 0))
                        .val(sum);
            } catch (EntityNotFoundException | LazyInitializationException e) {
                netDurationRepository.save(new NetDuration(
                        task,
                        0,
                        sum));
            }
        }
    }
}
