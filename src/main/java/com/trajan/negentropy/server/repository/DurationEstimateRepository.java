package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.DurationEstimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DurationEstimateRepository extends JpaRepository<DurationEstimate, Long>, JpaSpecificationExecutor<DurationEstimate> {
    DurationEstimate findOneByTaskIdAndPriority(long taskId, int priority);
}
