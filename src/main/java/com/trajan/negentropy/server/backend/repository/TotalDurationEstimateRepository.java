package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.totalduration.TotalDurationEstimate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * TimeEstimateRepository is a repository interface for managing TimeEstimate entities.
 */
@Repository("timeEstimateRepository")
public interface TotalDurationEstimateRepository extends BaseRepository<TotalDurationEstimate, Long> {
    List<TotalDurationEstimate> findByTaskIn(Collection<TaskEntity> tasks);
}
