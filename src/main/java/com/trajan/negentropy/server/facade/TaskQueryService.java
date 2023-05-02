package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import com.trajan.negentropy.server.facade.model.LinkID;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskID;
import com.trajan.negentropy.server.facade.model.TaskNode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * A Facade for the TaskEntityQueryService, returning DTO model objects instead of persisted Entity objects
 * @see TaskEntityQueryService
 */
@Service
public interface TaskQueryService {

    /**
     * Get a task by ID.
     *
     * @param taskId The ID of the task link to retrieve.
     * @return The matching Task.
     * @throws java.util.NoSuchElementException if no matching task was found with that ID.
     */
    Task getTask(TaskID taskId);

    /**
     * Get a task node by link ID.
     * @param nodeId The ID of the task link to retrieve a node from.
     * @return A task node object representing the task link with the corresponding ID.
     * @throws java.util.NoSuchElementException if no matching task link was found with that ID.
     */
    TaskNode getNode(LinkID nodeId);

    /**
     * Get a task node by parent task ID and position.
     * @param position The position (index) of the node to retrieve.
     * @param parentTaskId The ID of the task to retrieve a child node from.
     * @return A task node object representing the task link with the corresponding parent task and position.
     * @throws java.util.NoSuchElementException if no matching parent task was found with that ID.
     */
    TaskNode getNodeByPosition(int position, TaskID parentTaskId);

    /**
     * Get all tasks that meet a set of filters and tags.
     * @param filters A list of filters to apply to the tasks.
     * @param tagIds A set of tags, by ID, to filter the tasks.
     * @return A list of tasks that meet the specified filters and tags.
     */
    List<Task> findTasks(Iterable<Filter> filters, Iterable<Long> tagIds);

    /**
     * Counts the child tasks of a task by ID.
     * @param parentTaskId The parent task ID.
     * @return The number of child tasks.
     */
    int getChildCount(TaskID parentTaskId);

    /**
     * Checks if the task has children by parent ID.
     * @param parentTaskId The parent task ID.
     * @return True if the task has children, otherwise false.
     */
    boolean hasChildren(TaskID parentTaskId);

    /**
     * Retrieves the child task nodes of a particular task by ID, ordered.
     * @param parentTaskId The parent task ID.
     * @return An ordered list of child task nodes.
     */
    List<TaskNode> getChildNodes(TaskID parentTaskId);

    /**
     * Counts the tasks that have an associated parent of 'null'.
     * @return The number of root tasks.
     */
    int getRootCount();

    /**
     * Returns the task nodes that have an associated parent of 'null'.
     * @return An unordered list of root nodes
     */
    List<TaskNode> getRootNodes();

    /**
     * Retrieves the total duration of a task by ID, including all descendants.
     * @param taskId The ID of the task to find the duration for.
     * @return The total duration of a Task.
     */
    Duration getEstimatedTotalDuration(TaskID taskId);
}
