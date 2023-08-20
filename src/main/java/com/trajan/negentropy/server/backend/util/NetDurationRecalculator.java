package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.NetDurationID;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Component
@Transactional
public class NetDurationRecalculator {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationRepository netDurationRepository;

    public void recalculateTimeEstimates() {
        netDurationRepository.findAll()
                .forEach(estimate -> estimate.netDuration(Duration.ZERO));

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
                        .netDuration(sum);
            } catch (EntityNotFoundException e) {
                netDurationRepository.save(new NetDuration(
                        task,
                        0,
                        sum));
            }
        }
    }
}
