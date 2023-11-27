package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.shared.Registration;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface RoutineService {
    RoutineID activeRoutineId();

    Routine fetchRoutine(RoutineID routineID);
    RoutineStep fetchRoutineStep(StepID stepID);

    RoutineResponse createRoutine(TaskID rootId);
    RoutineResponse createRoutine(LinkID rootId);
    RoutineResponse createRoutine(TaskID rootId, TaskNodeTreeFilter filter);
    RoutineResponse createRoutine(LinkID rootId, TaskNodeTreeFilter filter);
    RoutineResponse createRoutine(List<TaskOrLinkID> rootIds, TaskNodeTreeFilter filter);

    long countCurrentRoutines(Set<TimeableStatus> statusSet);
    Stream<Routine> fetchRoutines(Set<TimeableStatus> statusSet);

    RoutineResponse startStep(StepID stepId, LocalDateTime time);
    RoutineResponse suspendStep(StepID stepId, LocalDateTime time);
    RoutineResponse jumpToStep(StepID stepId, LocalDateTime time);
    boolean completeStepWouldFinishRoutine(StepID stepId);
    RoutineResponse completeStep(StepID stepId, LocalDateTime time);
    RoutineResponse skipStep(StepID stepId, LocalDateTime time);
    RoutineResponse postponeStep(StepID stepId, LocalDateTime time);
    RoutineResponse excludeStep(StepID stepID, LocalDateTime time);

    RoutineResponse previousStep(StepID stepId, LocalDateTime time);

    RoutineResponse skipRoutine(RoutineID routineId, LocalDateTime now);

    RoutineResponse moveStep(StepID childId, StepID parentId, int position);

    RoutineResponse setStepExcluded(StepID stepId, LocalDateTime time, boolean exclude);

    RoutineResponse setAutoSync(RoutineID routineId, boolean autoSync);

    Registration register(RoutineID routineId, Consumer<Routine> listener);

    void notifyChanges(Request request, MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults);

    boolean hasFilteredOutSteps(RoutineID routineId);
}