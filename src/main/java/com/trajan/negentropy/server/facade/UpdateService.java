package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.NodeResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TagResponse;
import com.trajan.negentropy.server.facade.response.TaskResponse;

public interface UpdateService {

    /**
     * Adds a task as a child of a given parent task, inserting it at a specific index with the provided importance.
     * </p>
     * If a value is not provided, defaults will be used.
     * Automatically adjusts time estimates for the saved task's ancestors.
     * Will add a task as a root task with no position information if parent is null.
     *
     * @param fresh The task node dto to insert.
     * @return A task node object containing the created node.
     */
    NodeResponse insertTaskNode(TaskNodeDTO fresh);

    /**
     * Creates a new task entity.
     * If the Task already exists in the repository, returns a failure and does nothing.
     * </p>
     * A new node should be immediately created following creating a task, as the new task starts as an orphan.
     *
     * @param task The task to be created.
     * @return A response with the id of the created Task object.
     */
    TaskResponse createTask(Task task);

    /**
     * Updates the fields of an existing task.
     * If the Task does not yet exist in the repository, returns a failure and does nothing.
     * </p>
     * Automatically adjusts time estimates for the updated task's ancestors.
     *
     * @param task The task to be updated.
     * @return A response with the updated Task object.
     */
    TaskResponse updateTask(Task task);

    /**
     * Updates the fields of an existing task node.
     * If the task or task node does not yet exist in the repository, returns a failure and does nothing.
     * </p>
     *
     * @param node The task node to be updated.
     * @return A task node object containing the updated node, and the child and parent tasks.
     */
    NodeResponse updateNode(TaskNode node);

    /**
     * !! NOT YET IMPLEMENTED !!
     * </p>
     * Deletes a Task, including all of its children and subtasks, and relevant TaskLinks.
     * TODO: If the Task is referenced in historical data, just marks the task as 'deleted'.
     * TODO: Will also delete all Tasks that have no reference links as part of cleanup.
     * TODO: Will also remove all tags that no longer refer to a node.
     * Automatically adjusts time estimates for the deleted task's ancestors.
     *
     * @param taskId The ID of the Task to be deleted.
     * @return A response with no additional object payload.
     */
    Response deleteTask(TaskID taskId);

    /**
     * Deletes a task link/node, removing a relation between two tasks.
     * @param linkId The ID of the task node/link to be deleted.
     * @return A response with no additional object payload.
     */
    Response deleteNode(LinkID linkId);

    /**
     * Creates a new tag entity
     * @param tag The tag to persist.
     * @return A response containing the persisted tag.
     */
    TagResponse createTag(Tag tag);

    /**
     * Creates a new tag entity, or returns an existing tag if it already exists.
     * @param name The name of the tag to persist or retrieve.
     * @return A response containing the persisted tag.
     */
    TagResponse findTagOrElseCreate(String name);

    void recalculateTimeEstimates();
}
