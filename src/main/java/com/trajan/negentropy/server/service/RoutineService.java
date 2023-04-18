package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskSession;

public interface RoutineService {
    /**
     * Starts a task and records the start time. If the task is already running or paused, this method does nothing.
     *
     * @param nodeId The ID of the TaskNode to start.
     * @return A new TaskSession object containing the created TaskSession.
     * @throws IllegalArgumentException If the task is not found by the TaskService.
     */
    TaskSession startTask(long nodeId);

    /**
     * Pauses a running task and records the pause time. If the task is not running, this method does nothing.
     *
     * @param sessionId The ID of the TaskSession to pause.
     * @throws IllegalArgumentException If the TaskSession is not found .
     */
    void pauseTask(long sessionId);

    /**
     * Resumes a paused task and updates the total paused duration. If the task is not paused, this method does nothing.
     *
     * @param sessionId The ID of the TaskSession to resume.
     * @throws IllegalArgumentException If the TaskSession is not founds.
     */
    void resumeTask(long sessionId);

    /**
     * Marks a task as completed, regardless of its current status (running or paused).
     * If the task is not in the active tasks, this method does nothing.
     *
     * @param sessionId The ID of the TaskSession to mark as completed.
     * @throws IllegalArgumentException If the TaskSession is not found.
     */
    void completeTask(long sessionId);

    /**
     * Retrieves the current status, start time, pause time, and total paused duration of a task.
     * If the task is not in the active tasks, this method throws an IllegalArgumentException.
     *
     * @param sessionId The ID of the TaskSession to get session information for.
     * @return An TaskSession object containing the requested information.
     * @throws IllegalArgumentException If the TaskSession is not found.
     */
    TaskSession getTaskSession(long sessionId);

    /**
     * Gets the currently active task across all sessions.
     * If a task is already active, return that task; otherwise, return null.
     *
     * @return A TaskSession object if there's an active task, or null if no task is active.
     */
    TaskSession getActiveTaskSession();
}

