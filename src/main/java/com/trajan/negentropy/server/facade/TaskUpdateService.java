package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.response.LinkResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;

public interface TaskUpdateService {

    /**
     * Adds a task as a root task with no parents.
     * If the task does not yet exist in the repository, it will be created.
     *
     * @param fresh The Task to be added.
     * @return A response with the updated Task and TaskLink objects.
     */
    LinkResponse addTaskAsRoot(Task fresh);

    /**
     * Adds a task as a child of a given parent task, appending it to the parent tasks' children.
     * If the task does not yet exist in the repository, it will be created.
     * Automatically adjusts time estimates for the saved task's ancestors.
     *
     * @param child The task to be added.
     * @param parentId The ID of the parent task.
     * @return A response with the updated Task (parent and child) and TaskLink objects.
     */
    LinkResponse addTaskAsChild(long parentId, Task child);

    /**
     * Adds a task as a child of a given parent task, inserting it at a specific index.
     * If the task does not yet exist in the repository, it will be created.
     * Automatically adjusts time estimates for the saved task's ancestors.
     *
     * @param child The task to be added.
     * @param parentId The ID of the parent task.
     * @param index The specified position to insert the task at.
     * @return A response with the updated Task (parent and child) and TaskLink objects.
     */
    LinkResponse addTaskAsChildAt(int index, long parentId, Task child);

    /**
     * Updates the fields of an existing task.
     * If the Task does not yet exist in the repository, returns a failure and does nothing.
     * Automatically adjusts time estimates for the updated task's ancestors.
     *
     * @param task The task to be added.
     * @return A response with the updated Task object.
     */
    TaskResponse updateTask(Task task);

    /**
     * Deletes a Task, including all of its children and subtasks, and relevant TaskLinks.
     * TODO: If the Task is referenced in historical data, just marks the task as 'deleted'.
     * Will also delete all Tasks that have no reference links as part of cleanup.
     * Automatically adjusts time estimates for the deleted task's ancestors.
     *
     * @param taskId The ID of the Task to be deleted.
     * @return A response with no additional object payload.
     */
    Response deleteTask(long taskId);

    /**
     * Deletes a task link, removing a relation between two tasks.
     * @param linkId  The ID of the TaskLink to be deleted.
     * @return A response with the updated Task objects (parent and child).
     */
    LinkResponse deleteLink(long linkId);
}
