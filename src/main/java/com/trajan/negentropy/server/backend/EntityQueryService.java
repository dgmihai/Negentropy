package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.*;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.*;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A service providing fetching query access to the task and task link repository layer.
 * @see TaskRepository
 * @see LinkRepository
 */
public interface EntityQueryService {

    /**
     * Get a task entity from the repository by ID.
     *
     * @param taskId The ID of the task to retrieve.
     * @return The matching TaskEntity.
     * @throws java.util.NoSuchElementException if no matching entity was found with that ID.
     * @see TaskEntity
     */
    TaskEntity getTask(TaskID taskId);

    /**
     * Get a task link entity from the repository by ID.
     *
     * @param linkId The ID of the task link to retrieve.
     * @return The matching TaskLink.
     * @throws java.util.NoSuchElementException if no matching entity was found with that ID.
     * @see TaskLink
     */
    TaskLink getLink(LinkID linkId);

    /**
     * Get a tag entity from the repository by ID.
     *
     * @param tagId The ID of the tag to retrieve.
     * @return The matching tag entity.
     * @throws java.util.NoSuchElementException if no matching entity was found with that ID.
     * @see TaskLink
     */
    TagEntity getTag(TagID tagId);

    RoutineEntity getRoutine(RoutineID routineId);

    RoutineStepEntity getRoutineStep(StepID stepId);

    /**
     * Find a tag entity from the repository by name.
     *
     * @param name The name of the tag to retrieve.
     * @return An optional of the matching tag entity.
     * @see TaskLink
     */
    Optional<TagEntity> findTag(String name);

    /**
     * Finds tasks.
     *
     * @param filter The filter criteria to be applied.
     * @return A stream of TaskEntity objects in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     */
    Stream<TaskEntity> findTasks(TaskFilter filter);

    /**
     * Counts the child tasks of a task.
     *
     * @param parentId The ID of parent task, which can be null.
     * @param filter The filter criteria to be applied.
     * @return The number of child tasks given the filter criteria.
     * @see TaskFilter
     * @see TaskEntity
     */
    int findChildCount(TaskID parentId, TaskFilter filter);

    /**
     * Checks if the task has children.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @return True if the task has children, otherwise false.
     * @see TaskFilter
     * @see TaskEntity
     */
    boolean hasChildren(TaskID parentId, TaskFilter filter);

    /**
     * Retrieves all the child task links where a given task is a parent.
     * </p>
     * The returned stream is ordered by position.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of TaskLink entity objects in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findChildLinks(TaskID parentId, TaskFilter filter);

    /**
     * Retrieves all the child tasks where a given task is a parent.
     * </p>
     * The returned stream is ordered by position.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of TaskEntity objects in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     */
    Stream<TaskEntity> findChildTasks(TaskID parentId, TaskFilter filter);

    /**
     * Retrieves all the parent task links where a given task is a child.
     * </p>
     * The returned stream is unordered.
     *
     * @param childId The ID of the child task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of TaskLink entity objects in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findParentLinks(TaskID childId, TaskFilter filter);

    /**
     * Retrieves all the parent tasks where a given task is a child.
     * </p>
     * The returned stream is unordered.
     *
     * @param childId The ID of the child task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of TaskEntity objects in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     */
    Stream<TaskEntity> findParentTasks(TaskID childId, TaskFilter filter);

    /**
     * Retrieves all ancestor task links of a given task via depth-first search..
     * </p>
     * The returned stream is unordered and will include 'null' if one or more ancestors is a root task.
     *
     * @param descendantId The ID of the descendant task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of all ancestor TaskEntity objects, all in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findAncestorLinks(TaskID descendantId, TaskFilter filter);

    /**
     * Retrieves all ancestor tasks of a given task via depth-first search.
     * </p>
     * The returned stream is unordered and will include 'null' if one or more ancestors is a root task.
     *
     * @param descendantId The ID of the descendant task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of all ancestor TaskEntity objects, all in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskEntity> findAncestorTasks(TaskID descendantId, TaskFilter filter);

    /**
     * Retrieves all descendant task links of a given task via depth-first search.
     * </p>
     * The returned stream is ordered by parent, by position.
     *
     * @param ancestorId The ID of the ancestor task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of all descendant TaskLink entity objects, all in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskFilter filter);

    Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskFilter filter, Consumer<TaskLink> consumer);

    /**
     * Retrieves all descendant tasks of a given task via depth-first search.
     * </p>
     * The returned stream is ordered by parent, by position.
     *
     * @param ancestorId The ID of the ancestor task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of all descendant TaskEntity objects, all in the persistence context.
     * @see TaskFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskEntity> findDescendantTasks(TaskID ancestorId, TaskFilter filter);

    TotalDurationEstimate getTotalDuration(TaskID taskId);

    TotalDurationEstimate getTotalDuration(TaskID taskId, int importance);

    Stream<TotalDurationEstimate> getTotalDurationWithImportanceThreshold(TaskID taskId, int importanceDifference);

    Duration calculateTotalDuration(TaskID taskId, TaskFilter filter);

    int getLowestImportanceOfDescendants(TaskID ancestorId);

    Stream<TaskLink> findLeafTaskLinks(TaskFilter filter);

    Stream<TaskEntity> findOrphanedTasks();

    Stream<TagEntity> findOrphanedTags();
}
