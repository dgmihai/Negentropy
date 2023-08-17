package com.trajan.negentropy.server.facade;//package com.trajan.negentropy.server.facade;
//
//import com.trajan.negentropy.model.*;
//import com.trajan.negentropy.model.filter.TaskFilter;
//import com.trajan.negentropy.model.changes.LinkID;
//import com.trajan.negentropy.model.changes.TaskID;
//import com.trajan.negentropy.server.facade.response.*;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//public interface UpdateService {
//
//    /**
//     * Get a sync response containing all nodes and links to refresh by ID.
//     *
//     * @param syncId The ID of the sync changeset to begin from.
//     * @return A sync response containing all nodes and links to refresh.
//     */
//    SyncResponseOld sync(UUID syncId);
//
//    /**
//     * Adds a task as a child of a given parent task, inserting it at a specific index with the provided importance.
//     * </p>
//     * If a value is not provided, defaults will be used.
//     * Automatically adjusts time estimates for the saved task's ancestors.
//     * Will add a task as a root task with no position information if parent is null.
//     *
//     * @param fresh The task node dto to insert.
//     * @return A task node object containing the created node.
//     */
//    NodeResponse insertNode(TaskNodeDTO fresh);
//
//    /**
//     * Creates a new task entity.
//     * If the Task already exists in the repository, returns a failure and does nothing.
//     * </p>
//     * A new node should be immediately created following creating a task, as the new task starts as an orphan.
//     *
//     * @param task The task to be created.
//     * @return A response with the changes of the created Task object.
//     */
//    TaskResponse createTask(Task task);
//
//    /**
//     * Updates the fields of an existing task.
//     * If the Task does not yet exist in the repository, returns a failure and does nothing.
//     * </p>
//     * Automatically adjusts time estimates for the updated task's ancestors.
//     *
//     * @param task The task to be updated.
//     * @return A response with the updated Task object.
//     */
//    TaskResponse updateTask(Task task);
//
//    /**
//     * Updates the fields of an existing task node.
//     * If the task or task node does not yet exist in the repository, returns a failure and does nothing.
//     * </p>
//     *
//     * @param node The task node to be updated.
//     * @return A task node object containing the updated node, and the child and parent tasks.
//     */
//    NodeResponse updateNode(TaskNode node);
//
//    /**
//     * Updates the fields of all existing task nodes in the provided group of ids.
//     * If the task or task node does not yet exist in the repository, returns a failure and does nothing.
//     * </p>
//     *
//     * @param linkIds The group of linkIds to try and update.
//     * @param nodeInfo Common node information to update the corresponding task nodes with.
//     * @return A stream of the updated task nodes.
//     */
//    NodesResponse updateNodes(Iterable<LinkID> linkIds, GenericTaskNodeInfo nodeInfo);
//
//    /**
//     * !! NOT YET IMPLEMENTED !!
//     * </p>
//     * Deletes a Task, including all of its children and subtasks, and relevant TaskLinks.
//     * TODO: If the Task is referenced in historical changeRelevantDataMap, just marks the task as 'deleted'.
//     * TODO: Will also delete all Tasks that have no reference links as part of cleanup.
//     * TODO: Will also remove all tags that no longer refer to a node.
//     * Automatically adjusts time estimates for the deleted task's ancestors.
//     *
//     * @param taskId The ID of the Task to be deleted.
//     * @return A response with no additional object payload.
//     */
//    Response deleteTask(TaskID taskId);
//
//    /**
//     * Deletes a task link/node, removing a relation between two tasks.
//     * @param originalId The ID of the task node/link to be deleted.
//     * @return A response with no additional object payload.
//     */
//    Response deleteNode(LinkID originalId);
//
//    NodeResponse setLinkScheduledFor(LinkID originalId, LocalDateTime manualScheduledFor);
//
//    /**
//     * Creates a new tag entity
//     * @param tag The tag to persist.
//     * @return A response containing the persisted tag.
//     */
//    TagResponse createTag(Tag tag);
//
//    /**
//     * Creates a new tag entity, or returns an existing tag if it already exists.
//     * @param name The name of the tag to persist or retrieve.
//     * @return A response containing the persisted tag.
//     */
//    TagResponse findTagOrElseCreate(String name);
//
//    NodesResponse deepCopyTaskNode(TaskNodeDTO original, TaskFilter filter);
//
//    NodesResponse deepCopyTaskNode(TaskNodeDTO original, TaskFilter filter, String suffix);
//
//    void recalculateTimeEstimates();
//}
