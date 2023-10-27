package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.shared.Registration;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface RoutineService {
    Routine fetchRoutine(RoutineID routineIFD);
    RoutineStep fetchRoutineStep(StepID stepID);

    RoutineResponse createRoutine(TaskID rootId);
    RoutineResponse createRoutine(LinkID rootId);

    RoutineResponse recalculateRoutine(@NotNull RoutineID routineId);

    RoutineResponse createRoutine(TaskID rootId, TaskNodeTreeFilter filter);
    RoutineResponse createRoutine(LinkID rootId, TaskNodeTreeFilter filter);

    long countCurrentRoutines(Set<TimeableStatus> statusSet);
    Stream<Routine> fetchRoutines(Set<TimeableStatus> statusSet);

    RoutineResponse startStep(StepID stepId, LocalDateTime time);
    RoutineResponse suspendStep(StepID stepId, LocalDateTime time);
    RoutineResponse completeStep(StepID stepId, LocalDateTime time);
    RoutineResponse skipStep(StepID stepId, LocalDateTime time);

    RoutineResponse postponeStep(StepID stepId, LocalDateTime time);

    RoutineResponse previousStep(StepID stepId, LocalDateTime time);

    RoutineResponse skipRoutine(RoutineID routineId, LocalDateTime now);

    RoutineResponse moveStep(StepID childId, StepID parentId, int position);

    RoutineResponse setStepExcluded(StepID stepId, LocalDateTime time, boolean exclude);

    RoutineResponse setAutoSync(RoutineID routineId, boolean autoSync);

    Registration register(RoutineID routineId, Consumer<Routine> listener);

    void notifyChanges(Set<TaskLink> durationUpdates);
}