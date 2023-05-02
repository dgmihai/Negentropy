package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.facade.model.LinkID;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskID;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.response.NodeResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;

public interface TaskUpdateService {

    /**
     * Adds a task as a root task with no parents.
     *
     * @param freshId The ID of the task to be added.
     * @return A task node object containing the new node, the child task, and a null parent task.
     */
    NodeResponse insertTaskAsRoot(TaskID freshId);

    /**
     * Adds a task as a child of a given parent task, appending it to the parent tasks' children.
     * </p>
     * Automatically adjusts time estimates for the saved task's ancestors.
     * Will add a task as a root task if parent is null.
     *
     * @param childId The ID of the task to be added.
     * @param parentId The ID of the parent task.
     * @return A task node object containing the new node, and the child and parent tasks.
     */
    NodeResponse insertTaskAsChild(TaskID parentId, TaskID childId);

    /**
     * Adds a task as a child of a given parent task, inserting it at a specific index.
     * </p>
     * Automatically adjusts time estimates for the saved task's ancestors.
     * Will add a task as a root task with no position information if parent is null.
     *
     * @param childId The ID of the task to be added.
     * @param parentId The ID of the parent task.
     * @param index The specified position to insert the task at.
     * @return A task node object containing the new node, and the child and parent tasks.
     */
    NodeResponse insertTaskAsChildAt(int index, TaskID parentId, TaskID childId);

    /**
     * Adds a task before a task node, under the same parent.
     * </p>
     * Automatically adjusts time estimates for the saved task's ancestors.
     * If the parent is null (the next task node is a root node), does not persist order.
     *
     * @param freshId The ID of the task to be added.
     * @param nextId The ID of the task node to insert the fresh task before.
     * @return A task node object containing the new node, and the child and parent tasks.
     */
    NodeResponse insertTaskBefore(TaskID freshId, LinkID nextId);

    /**
     * Adds a task after a task node, under the same parent.
     * </p>
     * Automatically adjusts time estimates for the saved task's ancestors.
     * If the parent is null (the previous task node is a root node), does not persist order.
     *
     * @param freshId The ID of the task to be added.
     * @param prevId The ID of the task node to insert the fresh task after.
     * @return A task node object containing the new node, and the child and parent tasks.
     */
    NodeResponse insertTaskAfter(TaskID freshId, LinkID prevId);

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
     * Automatically adjusts time estimates for the updated task node's ancestors.
     *
     * @param node The task node to be updated.
     * @return A task node object containing the updated node, and the child and parent tasks.
     */
    NodeResponse updateNode(TaskNode node);

    /**
     * !! NOT YET IMPLEMENTED !!
     * Deletes a Task, including all of its children and subtasks, and relevant TaskLinks.
     * TODO: If the Task is referenced in historical data, just marks the task as 'deleted'.
     * Will also delete all Tasks that have no reference links as part of cleanup.
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
}
