package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.facade.model.EntityMapper;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskLink;
import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * A Facade for the TaskEntityQueryService, returning DTO model objects instead of persisted Entity objects
 * @see TaskEntityQueryService
 */
@Service
public class TaskQueryService {

    @Autowired
    TaskEntityQueryService entityQueryService;

    /**
     * Get a task by ID.
     *
     * @param taskId The ID of the task link to retrieve.
     * @return The matching Task.
     * @throws java.util.NoSuchElementException if no matching task was found with that ID.
     */
    public Task getTask(long taskId) {
        return EntityMapper.toDTO(entityQueryService.getTask(taskId));
    }

    /**
     * Get a task link by ID.
     *
     * @param linkId The ID of the task link to retrieve.
     * @return The matching Task.
     * @throws java.util.NoSuchElementException if no matching task was found with that ID.
     */
    public TaskLink getLink(long linkId) {
        return EntityMapper.toDTO(entityQueryService.getLink(linkId));
    }

    /**
     * Retrieves the total duration of a task, including all descendants.
     * @param taskId The ID of the task to find the duration for.
     * @return The total duration of a Task.
     */
    Duration getEstimatedTotalDuration(long taskId) {
        return entityQueryService.getEstimatedTotalDuration(taskId);
    }
}
