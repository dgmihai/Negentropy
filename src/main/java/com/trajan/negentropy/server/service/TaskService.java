/**
 * Service for managing Task and TaskNode entities.
 */
package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public interface TaskService {
    /**
     * Creates a new Task entity to persist.
     * </p>
     * One or more TaskNode entities should be created alongside the Task for it to be visible..
     *
     * @param task The Task to create the Task entity from.
     * @return The persisted Task entity.
     * @see Task
     * @see TaskNode
     */
    @Transactional
    Task createTask(Task task);

    /**
     * Updates an existing Task entity.
     *
     * @param task The Task entity to be updated.
     * @return The merged Task object.
     * @see Task
     */
    @Transactional
    Task updateTask(Task task);

    /**
     * Retrieves a Task object by its unique ID.
     *
     * @param taskId The ID of the Task entity to find.
     * @return The retrieved Task entity wrapped in an Optional.
     * @throws NoSuchElementException if a TaskNode with the associated ID does not exist.
     * @throws IllegalArgumentException if provided ID is null
     * @see Task
     */
    Optional<Task> getTask(long taskId);

    /**
     * Finds a list of Task entities using a list of filters.
     * </p>
     * Will NOT include the root Task.
     *
     * @param filters the list of filters to apply.
     * @return a list of Task entities that match the given filters.
     * @see Task
     * @see Filter
     */
    List<Task> findTasks(List<Filter> filters);

    /**
     * Deletes a Task entity.
     * </p>
     * WARNING: This also deletes all corresponding TaskNode entities.
     *
     * @param taskId The ID of the Task to be deleted.
     * @see Task
     * @see Filter
     * @see TaskNode
     */
    void deleteTask(long taskId);

//    /**
//     * Retrieves the root TaskNode entity, creating it if necessary.
//     * </p>
//     * This is the base for all TaskNodes in the hierarchy tree.
//     *
//     * @return the root TaskNode entity.
//     * @see TaskNode
//     */
//    TaskNode getRootNode();

    /**
     * Retrieves the ordered list of TaskNode entities that are children of the Task with the given ID.
     *
     * @param taskId The ID of the Task whose children are to be retrieved.
     * @return The list of TaskNode entities that are children of the Task.
     * @see Task
     * @see TaskNode
     */
    List<TaskNode> getChildNodes(long taskId);

    /**
     * Retrieves the set of TaskNode entities that represent parents Tasks of the Task with the given ID.
     *
     * @param taskId The ID of the Task whose associated parent TaskNodes are to be retrieved.
     * @return The set of parent TaskNodes entities of the Task with the given ID.
     * @see Task
     */
    Set<TaskNode> getParentNodes(long taskId);

    /**
     * Appends a new TaskNode entity to a Task's children.
     * </p>
     * Handles all ordering based on TaskNode.next.
     *
     * @param taskId The ID of the Task this TaskNode refers to.
     * @param parentTaskId The ID of the intended parent Task.
     * @return The persisted TaskNode entity.
     * @see TaskNode
     */
    @Transactional
    TaskNode appendNodeTo(long childTaskId, long parentTaskId);

    /**
     * Inserts a new TaskNode entity before the specified TaskNode.
     * </p>
     * Handles all ordering based on TaskNode.next.
     *
     * @param taskId The ID of the Task this TaskNode refers to.
     * @param nextNodeId The ID of the TaskNode that the new TaskNode will precede.
     * @return The persisted TaskNode entity.
     * @see TaskNode
     */
    @Transactional
    TaskNode insertNodeBefore(long taskId, long nextNodeId);

    /**
     * Retrieves a TaskNode object by its unique ID.
     *
     * @param nodeId The ID of the TaskNode entity to find.
     * @return The retrieved TaskNode entity wrapped in an Optional.
     * @throws NoSuchElementException if a TaskNodeNode with the associated ID does not exist.
     * @throws IllegalArgumentException if provided ID is null
     * @see TaskNode
     */
    Optional<TaskNode> getNode(long nodeId);

    /**
     * Deletes a TaskNode entity.
     *
     * @param nodeId The ID of the TaskNode to be deleted.
     * @see TaskNode
     * @see Filter
     */
    void deleteNode(long nodeId);
}
