package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;

public interface RoutineService {
    Routine fetchRoutine(RoutineID routineID);
    RoutineStep fetchRoutineStep(StepID stepID);

    RoutineResponse createRoutine(TaskID rootId);
    RoutineResponse createRoutine(TaskID rootId, TaskFilter filter);

    long countCurrentRoutines(Set<TimeableStatus> statusSet);
    Stream<Routine> fetchRoutines(Set<TimeableStatus> statusSet);

    RoutineResponse startStep(StepID stepId, LocalDateTime time);
    RoutineResponse suspendStep(StepID stepId, LocalDateTime time);
    RoutineResponse completeStep(StepID stepId, LocalDateTime time);
    RoutineResponse skipStep(StepID stepId, LocalDateTime time);
    RoutineResponse previousStep(StepID stepId, LocalDateTime time);
}