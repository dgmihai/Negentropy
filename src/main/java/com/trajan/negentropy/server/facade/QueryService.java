package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TagID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.stream.Stream;

/**
 * A Facade for the EntityQueryService, returning DTO model objects instead of persisted Entity objects
 * @see EntityQueryService
 */
@Service
public interface QueryService {

    /**
     * Get a task by ID.
     *
     * @param taskId The ID of the task link to retrieve.
     * @return The matching Task.
     * @throws java.util.NoSuchElementException if no matching task was found with that ID.
     */
    Task fetchTask(TaskID taskId);

    /**
     * Get a task node by link ID.
     *
     * @param nodeId The ID of the task link to retrieve a node from.
     * @return A task node object representing the task link with the corresponding ID.
     * @throws java.util.NoSuchElementException if no matching task link was found with that ID.
     */
    TaskNode fetchNode(LinkID nodeId);

    /**
     * Get all tasks that meet a set of filters and tags.
     *
     * @param filter The filter criteria to be applied.
     * @return A distinct stream of tasks that meet the specified filters and tags.
     */
    Stream<Task> fetchTasks(TaskFilter filter);

    /**
     * Counts the child tasks of a task by ID.
     *
     * @param parentTaskId The parent task ID.
     * @return The number of child tasks.
     */
    int fetchChildCount(TaskID parentTaskId);

    /**
     * Counts the child tasks of a task by ID.
     *
     * @param parentTaskId The parent task ID.
     * @param filter The filter criteria to be applied.
     * @return The number of child tasks.
     */
    int fetchChildCount(TaskID parentTaskId, TaskFilter filter);

    /**
     * Counts the child tasks of a task by ID.
     *
     * @param parentTaskId The parent task ID.
     * @param filter The filter criteria to be applied.
     * @param offset zero-based offset.
     * @param limit the size of the elements to be returned.
     * @return The number of child tasks.
     */
    int fetchChildCount(TaskID parentTaskId, TaskFilter filter, int offset, int limit);

    /**
     * Checks if the task has children by parent ID.
     *
     * @param parentTaskId The parent task ID.
     * @return True if the task has children, otherwise false.
     */
    boolean hasChildren(TaskID parentTaskId);

    /**
     * Checks if the task has children by parent ID.
     *
     * @param parentTaskId The parent task ID.
     * @param filter The filter criteria to be applied.
     * @return True if the task has children, otherwise false.
     */
    boolean hasChildren(TaskID parentTaskId, TaskFilter filter);

    /**
     * Retrieves the child task nodes of a particular task by ID, ordered.
     *
     * @param parentTaskId The parent task ID.
     * @return An ordered stream of child task nodes.
     */
    Stream<TaskNode> fetchChildNodes(TaskID parentTaskId);

    /**
     * Retrieves the child task nodes of a particular task by ID, ordered.
     *
     * @param parentTaskId The parent task ID.
     * @param filter The filter criteria to be applied.
     * @return An ordered stream of child task nodes.
     */
    Stream<TaskNode> fetchChildNodes(TaskID parentTaskId, TaskFilter filter);

    /**
     * Retrieves the child task nodes of a particular task by ID, ordered.
     *
     * @param parentTaskId The parent task ID.
     * @param filter The filter criteria to be applied.
     * @param offset zero-based offset.
     * @param limit the size of the elements to be returned.
     * @return An ordered stream of child task nodes.
     */
    Stream<TaskNode> fetchChildNodes(TaskID parentTaskId, TaskFilter filter, int offset, int limit);

    /**
     * Retrieves the total duration of a task by ID, including all descendants.
     *
     * @param taskId The ID of the task to find the duration for.
     * @param filter The filter criteria to be applied.
     * @return The total duration of a Task.
     */
    // TODO: Not yet implemented
//    Map<TaskID, Duration> fetchNetDurations(TaskID taskId, TaskFilter filter);

    int fetchRootCount();

    /**
     * Counts the tasks that have at least one instance associated with the root parent.
     *
     * @param filter The filter criteria to be applied.
     * @return The number of root tasks.
     */
    int fetchRootCount(TaskFilter filter);

    /**
     * Counts the tasks that have at least one instance associated with the root parent.
     *
     * @param filter The filter criteria to be applied.
     * @param offset zero-based offset.
     * @param limit the size of the elements to be returned.
     * @return The number of root tasks.
     */
    int fetchRootCount(TaskFilter filter, int offset, int limit);

    /**
     * Returns the task nodes associated with the root parent.
     *
     * @return An ordered stream of root nodes
     */
    Stream<TaskNode> fetchRootNodes();

    /**
     * Returns the task nodes associated with the root parent.
     *
     * @param filter The filter criteria to be applied.
     * @return An ordered stream of root nodes
     */
    Stream<TaskNode> fetchRootNodes(TaskFilter filter);

    /**
     * Returns the task nodes associated with the root parent.
     *
     * @param filter The filter criteria to be applied.
     * @param offset zero-based offset.
     * @param limit the size of the elements to be returned.
     * @return An ordered stream of root nodes
     */
    Stream<TaskNode> fetchRootNodes(TaskFilter filter, int offset, int limit);

    Stream<TaskNode> fetchDescendantNodes(TaskID ancestorId, TaskFilter filter);

    /**
     * Retrieves a tag by its ID.
     *
     * @param tagId The ID of the tag to retrieve.
     * @return A Tag object.
     */
    Tag fetchTag(TagID tagId);

    /**
     * Retrieves all initialized tags from the repository.
     *
     * @return A stream of all tags.
     */
    Stream<Tag> fetchAllTags();

    /**
     * Retrieves all tags that are associated with a particular task, by the task's ID.
     * @param taskId The ID of the task whose tags will be retrieved.
     * @return A stream of all tags associated with the given task.
     */
    Stream<Tag> fetchTags(TaskID taskId);

    Duration fetchNetTimeDuration(TaskID taskId);

    Duration fetchNetTimeDuration(TaskID taskId, int priority);

    Duration fetchNetTimeDuration(TaskID taskId, TaskFilter filter);

    int fetchLowestImportanceOfDescendants(TaskID ancestorId);
}
