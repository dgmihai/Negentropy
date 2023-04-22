/**
 * Service for managing Task and TaskNode entities.
 */
package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public interface TaskService {

    /**
     * Creates a new Task entity to persist along with a base associated TaskNode.
     *
     * @param task The Task to create the Task entity from.
     * @return A springframework Pair containing The persisted Task and TaskNode entities..
     * @throws IllegalArgumentException if the Task being created already has an assigned ID.
     * @see Task
     * @see TaskNode
     */
    @Transactional
    Pair<Task, TaskNode> createTaskWithNode(Task task);

    /**
     * Creates a new Task entity to persist.
     * </p>
     * One or more TaskNode entities should be created alongside the Task for it to be visible..
     *
     * @param task The Task to create the Task entity from.
     * @return The persisted Task entity.
     * @throws IllegalArgumentException if the Task being created already has an assigned ID.
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
     * @throws NullPointerException if provided ID is null
     * @see Task
     */
    Optional<Task> getTask(long taskId);

    /**
     * Finds a list of Task entities using a list of filters.
     *
     * @param filters the list of filters to apply.
     * @return a list of Task entities that match the given filters.
     * @see Filter
     * @see Task
     */
    List<Task> findTasks(List<Filter> filters);

    /**
     * Deletes a Task entity.
     * </p>
     * WARNING: This also deletes all corresponding TaskNode entities.
     *
     * @param taskId The ID of the Task to be deleted.
     * @see Filter
     * @see Task
     * @see TaskNode
     */
    void deleteTask(long taskId);

    /**
     * Retrieves the unordered list of TaskNode entities that have no parent Task.
     *
     * @return The list of TaskNode entities that have no parent Task associated with them.
     * @see Task
     * @see TaskNode
     */
    List<TaskNode> getOrphanNodes();

    /**
     * Retrieves the count of TaskNode entities that have no parent Task.
     *
     * @return The count of TaskNode entities that have no parent Task associated with them.
     * @see Task
     * @see TaskNode
     */
    int countOrphanNodes();

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
     * Retrieves the count of TaskNode entities that are children of the Task with the given ID.
     *
     * @param taskId The ID of the Task whose children are to be counted.
     * @return The count of TaskNode entities that are children of the Task.
     * @see Task
     * @see TaskNode
     */
    int countChildNodes(long taskId);

    /**
     * Retrieves the ordered list of TaskNode entities that are children of the Task with the given ID.
     * </p>
     * This also applies a set of filters to the TaskNodes retrieved, still in relative order.
     *
     * @param taskId The ID of the Task whose children are to be retrieved.
     * @param filters the list of filters to apply.
     * @return The list of TaskNode entities that are children of the Task.
     * @see Task
     * @see TaskNode
     */
    List<TaskNode> findChildNodes(long taskId, List<Filter> filters);

    /**
     * Retrieves the set of TaskNode entities that represent parents Tasks of the Task with the given ID.
     *
     * @param taskId The ID of the Task whose associated parent TaskNodes are to be retrieved.
     * @return The set of parent TaskNodes entities of the Task with the given ID.
     * @see Task
     */
    Set<TaskNode> getReferenceNodes(long taskId);

    /**
     * Appends a new TaskNode entity to a Task's children.
     * </p>
     * Appends to the end of parent's linked list of TaskNodes.
     *
     * @param childTaskId The ID of the Task this TaskNode refers to.
     * @param parentTaskId The ID of the intended parent Task.
     * @param priority The priority of the TaskNode.
     * @return The persisted TaskNode entity.
     * @throws NoSuchElementException if a Task with the relevant ID does not exist.
     * @see TaskNode
     */
    @Transactional
    TaskNode createChildNode(long parentTaskId, long childTaskId, int priority);

    /**
     * Inserts a new TaskNode entity before the specified TaskNode.
     * </p>
     * Handles all ordering based on the TaskNode doubly linked list.
     *
     * @param taskId The ID of the Task this TaskNode refers to.
     * @param nextNodeId The ID of the TaskNode that the new TaskNode will precede.
     * @param priority The priority of the TaskNode.
     * @return The persisted TaskNode entity.
     * @throws NoSuchElementException if a Task with the relevant ID does not exist.
     * @see TaskNode
     */
    @Transactional
    TaskNode createNodeBefore(long taskId, long nextNodeId, int priority);

    /**
     * Inserts a new TaskNode entity after the specified TaskNode.
     * </p>
     * Handles all ordering based on the TaskNode doubly linked list.
     *
     * @param taskId The ID of the Task this TaskNode refers to.
     * @param prevNodeId The ID of the TaskNode that the new TaskNode will succeed.
     * @param priority The priority of the TaskNode.
     * @return The persisted TaskNode entity.
     * @throws NoSuchElementException if a Task with the relevant ID does not exist.
     * @see TaskNode
     */
    @Transactional
    TaskNode createNodeAfter(long taskId, long prevNodeId, int priority);

    /**
     * Inserts a new TaskNode entity with no specified parent.
     * </p>
     * This TaskNode starts with no relationships.
     *
     * @param taskId The ID of the Task this TaskNode refers to.
     * @return The persisted TaskNode entity.
     * @throws NoSuchElementException if a Task with the relevant ID does not exist.
     * @see TaskNode
     */
    @Transactional
    TaskNode createOrphanNode(long taskId);

    /**
     * Retrieves a TaskNode object by its unique ID.
     *
     * @param nodeId The ID of the TaskNode entity to find.
     * @return The retrieved TaskNode entity wrapped in an Optional.
     * @throws NoSuchElementException if a TaskNodeNode with the associated ID does not exist.
     * @throws NullPointerException if provided ID is null
     * @see TaskNode
     */
    Optional<TaskNode> getNode(long nodeId);

    /**
     * Finds a list of TaskNode entities using a list of filters.
     * </p>
     *
     * @param filters the list of filters to apply.
     * @return a list of Task entities that match the given filters.
     * @see Filter
     * @see TaskNode
     */
    List<TaskNode> findAllNodes(List<Filter> filters);

    /**
     * Deletes a TaskNode entity.
     *
     * @param nodeId The ID of the TaskNode to be deleted.
     * @see Filter
     * @see TaskNode
     */
    void deleteNode(long nodeId);
}
