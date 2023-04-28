package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TimeEstimate;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TimeEstimateRepository is a repository interface for managing TimeEstimate entities.
 * It provides methods to query and manipulate TimeEstimate instances.
 */
@Repository
@Transactional
public interface TimeEstimateRepository extends JpaRepository<TimeEstimate, Long>, JpaSpecificationExecutor<TimeEstimate> {
    /**
     * Finds a TimeEstimate by its associated Task ID and priority.
     *
     * @param taskId   The ID of the associated Task.
     * @param priority The priority of the TimeEstimate.
     * @return The found TimeEstimate, or null if not found.
     */
    TimeEstimate findOneByTaskIdAndPriority(long taskId, int priority);

    /**
     * Finds all TimeEstimates by their associated Task.
     *
     * @param taskId The associated Task ID.
     * @return The found TimeEstimates.
     */
    List<TimeEstimate> findByTaskIdOrderByPriorityAsc(long taskId);

    /**
     * Finds a TimeEstimate by its associated Task ID.
     *
     * @param taskId The ID of the associated Task.
     * @return The found TimeEstimate, or null if not found.
     */
    TimeEstimate findOneByTaskId(long taskId);

    /**
     * Counts the number of TimeEstimates associated with a Task.
     *
     * @param task The associated Task.
     * @return The count of TimeEstimates associated with the Task.
     */
    int countByTask(TaskEntity task);

    /**
     * Deletes all TimeEstimates associated with a Task.
     *
     * @param task The associated Task.
     */
    void deleteAllByTask(TaskEntity task);
}
