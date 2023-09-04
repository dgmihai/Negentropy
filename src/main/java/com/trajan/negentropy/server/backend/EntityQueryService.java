package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.*;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
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

    Stream<TaskEntity> getTasks(Collection<TaskID> taskIds);

    /**
     * Get a task link entity from the repository by ID.
     *
     * @param linkId The ID of the task link to retrieve.
     * @return The matching TaskLink.
     * @throws java.util.NoSuchElementException if no matching entity was found with that ID.
     * @see TaskLink
     */
    TaskLink getLink(LinkID linkId);

    Stream<TaskLink> getLinks(Collection<LinkID> linkIds);

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
     * @see TaskTreeFilter
     * @see TaskEntity
     */
    Stream<TaskEntity> findTasks(TaskTreeFilter filter);

    Stream<TaskLink> findLinks(TaskTreeFilter filter);

    Stream<LinkID> findLinkIds(TaskTreeFilter filter);

    /**
     * Counts the child tasks of a task.
     *
     * @param parentId The ID of parent task, which can be null.
     * @param filter The filter criteria to be applied.
     * @return The number of child tasks given the filter criteria.
     * @see TaskTreeFilter
     * @see TaskEntity
     */
    int findChildCount(TaskID parentId, TaskTreeFilter filter);

    /**
     * Counts the child tasks of a task.
     *
     * @param parentId The ID of parent task, which can be null.
     * @param filter The filter criteria to be applied.
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     * @return The number of child tasks given the filter criteria.
     * @see TaskTreeFilter
     * @see TaskEntity
     */
    int findChildCount(TaskID parentId, TaskTreeFilter filter, int offset, int limit);

    /**
     * Checks if the task has children.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @return True if the task has children, otherwise false.
     * @see TaskTreeFilter
     * @see TaskEntity
     */
    boolean hasChildren(TaskID parentId, TaskTreeFilter filter);

    /**
     * Retrieves all the child task links where a given task is a parent.
     * </p>
     * The returned stream is ordered by position.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of TaskLink entity objects in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findChildLinks(TaskID parentId, TaskTreeFilter filter);

    /**
     * Retrieves all the child task links where a given task is a parent.
     * </p>
     * The returned stream is ordered by position.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     * @return A non-distinct ordered stream of TaskLink entity objects in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findChildLinks(TaskID parentId, TaskTreeFilter filter, int offset, int limit);

    /**
     * Retrieves all the child tasks where a given task is a parent.
     * </p>
     * The returned stream is ordered by position.
     *
     * @param parentId The ID of the parent task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of TaskEntity objects in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     */
    Stream<TaskEntity> findChildTasks(TaskID parentId, TaskTreeFilter filter);

    /**
     * Retrieves all the parent task links where a given task is a child.
     * </p>
     * The returned stream is unordered.
     *
     * @param childId The ID of the child task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of TaskLink entity objects in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findParentLinks(TaskID childId, TaskTreeFilter filter);

    /**
     * Retrieves all the parent tasks where a given task is a child.
     * </p>
     * The returned stream is unordered.
     *
     * @param childId The ID of the child task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of TaskEntity objects in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     */
    Stream<TaskEntity> findParentTasks(TaskID childId, TaskTreeFilter filter);

    /**
     * Retrieves all ancestor task links of a given task via depth-first search..
     * </p>
     * The returned stream is unordered and will include 'null' if one or more ancestors is a root task.
     *
     * @param descendantId The ID of the descendant task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of all ancestor TaskEntity objects, all in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findAncestorLinks(TaskID descendantId, TaskTreeFilter filter);

    /**
     * Retrieves all ancestor tasks of a given task via depth-first search.
     * </p>
     * The returned stream is unordered and will include 'null' if one or more ancestors is a root task.
     *
     * @param descendantId The ID of the descendant task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct unordered stream of all ancestor TaskEntity objects, all in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskEntity> findAncestorTasks(TaskID descendantId, TaskTreeFilter filter);

    /**
     * Retrieves all descendant task links of a given task via depth-first search.
     * </p>
     * The returned stream is ordered by parent, by position.
     *
     * @param ancestorId The ID of the ancestor task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of all descendant TaskLink entity objects, all in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskTreeFilter filter);
    Stream<TaskLink> findDescendantLinks(TaskID ancestorId, TaskTreeFilter filter, Consumer<TaskLink> consumer);

    Stream<TaskLink> findDescendantLinks(LinkID ancestorId, TaskTreeFilter filter);
    Stream<TaskLink> findDescendantLinks(LinkID ancestorId, TaskTreeFilter filter, Consumer<TaskLink> consumer);

    Stream<TaskLink> findDescendantTasksFromLink(LinkID linkId, TaskTreeFilter filter);

    Stream<TaskLink> findDescendantTasksFromLink(LinkID linkId, TaskTreeFilter filter, Consumer<TaskLink> consumer);

    /**
     * Retrieves all descendant tasks of a given task via depth-first search.
     * </p>
     * The returned stream is ordered by parent, by position.
     *
     * @param ancestorId The ID of the ancestor task.
     * @param filter The filter criteria to be applied.
     * @return A non-distinct ordered stream of all descendant TaskEntity objects, all in the persistence context.
     * @see TaskTreeFilter
     * @see TaskEntity
     * @see TaskLink
     */
    Stream<TaskEntity> findDescendantTasks(TaskID ancestorId, TaskTreeFilter filter);

    Map<TaskID, Duration> getAllNetDurations(TaskTreeFilter filter);

    NetDuration getNetDuration(TaskID taskId);

    NetDuration getNetDuration(TaskID taskId, int importance);

    Stream<NetDuration> getTotalDurationWithImportanceThreshold(TaskID taskId, int importanceDifference);

    Duration calculateNetDuration(TaskID taskId, TaskTreeFilter filter);

    int getLowestImportanceOfDescendants(TaskID ancestorId);

    Stream<TaskLink> findLeafTaskLinks(TaskTreeFilter filter);

    Stream<TaskEntity> findOrphanedTasks();

    Stream<TagEntity> findOrphanedTags();

    Stream<TaskEntity> findProjects();
}
