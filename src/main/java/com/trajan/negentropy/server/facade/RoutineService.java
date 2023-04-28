package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.entity.Routine;
import com.trajan.negentropy.server.backend.entity.RoutineStep;
import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.backend.entity.status.TaskStatus;

import java.util.List;

public interface RoutineService {
    /**
     * Starts a task and records the start time and records the active time.
     * </p>
     * If the task is already running or paused, this method does nothing.
     *
     * @param taskId The ID of the Task to create a Routine from.
     * TODO: @param priority The minimum priority level of subtasks to include
     * @return A new Routine object containing the created Routine.
     * @throws IllegalArgumentException If the task is not found by the TaskService.
     */
    Routine startNewRoutine(long taskId);

    /**
     * Completes the latest Step of a Routine and returns the next Step.
     * </p>
     * Calculates the elapsedActiveTime if the Task was ACTIVE.
     *
     * @param routineId The ID of the Routine.
     * @return The updated Routine.
     * @throws IllegalArgumentException If the Routine is not found by the RoutineService.
     */
    Routine completeStep(long routineId);

    /**
     * Skips the latest Step of a Routine and returns the next Step.
     * </p>
     * Calculates the elapsedActiveTime if the Task was ACTIVE.
     *
     * @param routineId The ID of the Routine.
     * @return The updated Routine.
     * @throws IllegalArgumentException If the Routine is not found by the RoutineService.
     */
    Routine skipStep(long routineId);

    /**
     * Suspends the latest Step of a Routine and returns that same Step.
     * </p>
     * Calculates the elapsedActiveTime if the Task was ACTIVE.
     *
     * @param routineId The ID of the Routine.
     * @return The updated Routine.
     * @throws IllegalArgumentException If the Routine is not found by the RoutineService.
     */
    Routine suspendStep(long routineId);

    /**
     * Resumes the latest Step of a Routine, setting it to ACTIVE.
     * </p>
     *
     * @param routineId The ID of the Routine.
     * @return The updated Routine.
     * @throws IllegalArgumentException If the Routine is not found by the RoutineService.
     */
    Routine resumeStep(long routineId);

    /**
     * Sets the TaskStatus of a particular RoutineStep.
     * </p>
     *
     * @param routineStepId The ID of the RoutineStep.
     * @return A new RoutineStep object containing the updated Step.
     * @throws IllegalArgumentException If the Routine is not found by the RoutineService.
     */
    Routine setRoutineStepStatus(long routineStepId, TaskStatus status);

    /**
     * Retrieves the Routine entity using the provided ID.
     * If the task is not in the active tasks, this method throws an IllegalArgumentException.
     *
     * @param routineId The ID of the Routine to get session information for.
     * @return A Routine object containing the requested information.
     * @throws IllegalArgumentException If the Routine is not found.
     */
    Routine getRoutine(long routineId);

    /**
     * Gets all Routines with a particular status.
     *
     * @param status The RoutineStatus to filter by.
     * @return A List of Routine objects with the matching RoutineStatus.
     */
    List<Routine> findRoutinesByStatus(RoutineStatus status);

    /**
     * Checks to see if any RoutineSteps refer to a particular Task, referenced by ID.
     *
     * @return Returns 'true' if any RoutineStep refers to a particular Task, 'false' otherwise.
     * @throws IllegalArgumentException If the Task is not found.
     */
    Boolean doesAnyRoutineStepReferToTask(long taskId);

    RoutineStep getCurrentStep(long routineId);
}