package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TotalDurationEstimate;
import org.springframework.stereotype.Repository;

/**
 * TimeEstimateRepository is a repository interface for managing TimeEstimate entities.
 */
@Repository("timeEstimateRepository")
public interface TotalDurationEstimateRepository extends BaseRepository<TotalDurationEstimate, Long> { }
