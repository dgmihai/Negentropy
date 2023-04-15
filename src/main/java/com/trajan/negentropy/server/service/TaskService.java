/**
 * Service for managing TaskInfo and TaskNode entities.
 */
package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;

import java.util.List;

public interface TaskService {
    /**
     * Saves a TaskInfo entity.
     * <p>
     * This does not persist the TaskInfo's associated TaskNodes. Save each TaskNode uniquely using saveNode.
     *
     * @param taskInfo the TaskInfo entity to save.
     * @return the saved TaskInfo entity.
     * @see TaskInfo
     * @see TaskNode saveTaskNode(TaskNode taskNode)
     */
    TaskInfo saveTaskInfo(TaskInfo taskInfo);

    /**
     * Finds a TaskInfo entity by ID.
     *
     * @param id the ID of the TaskInfo entity to find.
     * @return the TaskInfo entity with the given ID, or null if none is found.
     * @throws RuntimeException if a TaskNode with the associated ID does not exist.
     * @see TaskInfo
     */
    TaskInfo getTaskInfoById(Long id);

    /**
     * Finds a list of TaskInfo entities using a list of filters.
     * </p>
     * Will NOT include the root TaskInfo.
     *
     * @param filters the list of filters to apply.
     * @return a list of TaskInfo entities that match the given filters.
     * @see TaskNode
     * @see Filter
     */
    List<TaskInfo> findTaskInfos(List<Filter> filters);

    /**
     * Deletes a TaskInfo entity.
     *
     * @param taskInfo the TaskInfo entity to delete.
     * @see TaskInfo
     */
    void deleteTaskInfo(TaskInfo taskInfo);

    /**
     * Returns the root TaskInfo entity, creating it if necessary.
     *
     * @return the root TaskInfo entity.
     * @see TaskInfo
     */
    TaskInfo getRootTaskInfo();

    /**
     * Saves a TaskNode entity.
     * <p>
     *
     * @param taskNode the TaskNode entity to save, with ideally only the 'next' field set, and 'prev' as null.
     * @return the saved TaskNode entity.
     * @throws IllegalArgumentException if the TaskNode's 'next' or 'prev' don't share the same parent.
     * @see TaskNode
     */
    TaskNode saveTaskNode(TaskNode taskNode);

    /**
     * Finds a TaskNode entity by ID.
     *
     * @param id the ID of the TaskNode entity to find.
     * @return the TaskNode entity with the given ID, or null if none is found.
     * @throws RuntimeException if a TaskNode with the associated ID does not exist.
     * @see TaskNode
     */
    TaskNode getTaskNodeById(Long id);

    /**
     * Finds a list of TaskNode entities using a list of filters.
     *
     * @param filters the list of filters to apply.
     * @return a list of TaskNode entities that match the given filters.
     * @see TaskNode
     * @see Filter
     */
    public List<TaskNode> findTaskNodes(List<Filter> filters);

    /**
     * Deletes a TaskNode entity.
     *
     * @param taskNode the TaskNode entity to delete.
     * @see TaskNode
     */
    public void deleteTaskNode(TaskNode taskNode);
}
