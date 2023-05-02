package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.backend.repository.filter.Filter;

import java.time.Duration;
import java.util.stream.Stream;

/**
 * A service providing fetching gating access to the task repository layer
 * @see TaskRepository
 * @see LinkRepository
 */
public interface TaskEntityQueryService {

    /**
     * Get a task by ID.
     * @param taskId The ID of the task to retrieve.
     * @return The matching TaskEntity.
     * @throws java.util.NoSuchElementException if no matching entity was found with that ID.
     */
    TaskEntity getTask(long taskId);

    /**
     * Get a task link by ID.
     * @param linkId The ID of the task link to retrieve.
     * @return The matching TaskLink.
     * @throws java.util.NoSuchElementException if no matching entity was found with that ID.
     */
    TaskLink getLink(long linkId);


    /**
     * Get all tasks that meet a set of filters and tags.
     * @param filters A list of filters to apply to the tasks.
     * @param tags A set of tags to filter the tasks.
     * @return A stream of tasks that meet the specified filters and tags.
     */
    Stream<TaskEntity> findTasks(Iterable<Filter> filters, Iterable<TagEntity> tags);

    /**
     * Counts the child tasks of a task.
     * @param parentId The ID of parent task.
     * @return The number of child tasks.
     */
    int getChildCount(Long parentId);

    /**
     * Checks if the task has children.
     * @param parentId The ID of parent task.
     * @return True if the task has children, otherwise false.
     */
    boolean hasChildren(long parentId);

    /**
     * Retrieves all the task links where a task is a child.
     * The returned stream is unordered.
     * @param child The child task.
     * @return A stream of TaskLink entities.
     */
    Stream<TaskLink> getLinksByChild(TaskEntity child);

    /**
     * Retrieves all the task links where a task is a parent.
     * The returned stream is unordered.
     * @param parent The parent task.
     * @return A stream of TaskLink entities.
     */
    Stream<TaskLink> getLinksByParent(TaskEntity parent);

    /**
     * Retrieves all the task links where a given task is a parent.
     * The returned stream is unordered.
     * @param parentId The id of the parent task.
     * @return A stream of TaskLink entities.
     */
    Stream<TaskLink> getLinksByParentId(long parentId);

    /**
     * Checks if the task has parents.
     * @param parent The parent task.
     * @return True if the task has parents, otherwise false.
     */
    boolean hasParents(TaskEntity parent);

    /**
     * Retrieves all ancestor tasks of a task in an unordered stream.
     * </p>
     * Will include 'null' if one or more ancestors is a root task.
     * @param descendant The descendant task.
     * @return A stream of all ancestor Task entities.
     */
    Stream<TaskEntity> getAncestors(TaskEntity descendant);

    /**
     * Retrieves all ancestor task links of a task in an unordered stream.
     * </p>
     * Will include 'null' if one or more ancestors is a root task.
     * @param descendant The descendant task.
     * @return A stream of all ancestor TaskLink entities.
     */
    Stream<TaskLink> getAncestorLinks(TaskEntity descendant);

    /**
     * Retrieves all ancestor tasks of a task ordered via depth-first search.
     * @param ancestor The root, or ancestor task.
     * @return A stream of all descendant Task entities.
     */
    Stream<TaskEntity> getDescendants(TaskEntity ancestor);

    /**
     * Retrieves all ancestor task links of a task ordered via depth-first search.
     * @param ancestor The root, or ancestor task.
     * @return A stream of all descendant TaskLink entities.
     */
    Stream<TaskLink> getDescendantLinks(TaskEntity ancestor);

    /**
     * Retrieves the total duration of a task, including all descendants.
     * @param taskId The ID of the task to find the duration for.
     * @return The total duration of a Task.
     */
    Duration getEstimatedTotalDuration(long taskId);
}
