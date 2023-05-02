//package com.trajan.negentropy.server.service;
//
//import com.trajan.negentropy.server.entity.TaskEntity;
//import com.trajan.negentropy.server.entity.TaskLink;
//
//import java.time.Duration;
//
///**
// * TimeEstimator is responsible for maintaining the net time estimates of tasks.
// * It provides methods to handle the creation and deletion of tasks and their nodes,
// * and updates time estimates accordingly.
// */
//public interface TimeEstimator {
//
//    /**
//     * Handles the creation of a new TaskLink, updating the time estimates for each ancestor.
//     *
//     * @param node The newly created TaskLink.
//     */
//    void onLinkCreated(TaskLink node);
//
//    /**
//     * Handles the deletion of a TaskLink, updating the time estimates for each ancestor.
//     *
//     * @param node The TaskLink to be deleted.
//     */
//    void onLinkDeleted(TaskLink node);
//
//    /**
//     * Handles the creation of a new Task, creating a new TimeEstimate for it.
//     *
//     * @param task The newly created Task.
//     */
//    void initTaskTimeEstimate(TaskEntity task);
//
//    /**
//     * Handles the deletion of a Task, updating the time estimates for affected tasks
//     * and deleting the associated TimeEstimate.
//     *
//     * @param task The Task to be deleted.
//     */
//    void onTaskDeleted(TaskEntity task);
//
//    /**
//     * Updates the estimated total durations of a task and all ancestors of that task when a task's duration is updated.
//     *
//     * @param task The task that has been updated.
//     * @param difference The difference between the original and new durations.
//     */
//    void updateTaskTimeEstimateBy(TaskEntity task, Duration difference);
//
//    void updateAncestorTimeEstimateBy(TaskEntity task, Duration difference);
//}
