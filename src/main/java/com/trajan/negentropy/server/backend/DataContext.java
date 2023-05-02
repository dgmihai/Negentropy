package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;

import java.util.Set;

/**
 * A service implementation of the DataContext interface, managing persistence of entities.
 */
public interface DataContext {

    /**
     * Merge a detached Task entity to persistence.
     *
     * @param fresh The Task object to be saved.
     * @return The persisted Task entity.
     */
    // TODO: Unused
    TaskEntity updateTask(TaskEntity fresh);

    /**
     * Persists a transient, or new, Task entity.
     *
     * @param fresh The transient Task object to be persisted.
     * @return The persisted Task entity.
     */
    TaskEntity createTask(TaskEntity fresh);

    /**
     * Persists a transient, or new, TaskLink entity.
     *
     * @param fresh The transient TaskLink object to be persisted.
     * @return The persisted TaskLink entity.
     */
    TaskLink createLink(TaskLink fresh);

    /**
     * Updates a detached TaskLink entity to the repository.
     *
     * @param fresh The detached TaskLink object to be saved.
     * @return The persisted TaskLink entity.
     */
    // TODO: Unused
    TaskLink updateLink(TaskLink fresh);

    /**
     * Deletes a Task entity from the database.
     * Does NOT automatically adjust time estimates.
     *
     * @param task The Task entity to be deleted.
     */
    // TODO: Unused
    void deleteTask(TaskEntity task);

    /**
     * Deletes a collection of Task entities from the database in a batch.
     * Does NOT automatically adjust time estimates.
     *
     * @param tasks A set of Task entities to be deleted.
     */
    void deleteTasks(Set<TaskEntity> tasks);
}