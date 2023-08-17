package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.model.RoutineStep;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.RoutineResponse;

import java.util.List;

public interface ClientDataController {
    // TODO: Data provider for each grid
    UserSettings settings();
    SessionServices services();
    TaskEntryDataProviderManager taskEntryDataProviderManager();
    TaskNodeProvider activeTaskNodeProvider();

    DataMapResponse requestChange(Change change);
    DataMapResponse requestChanges(List<Change> changes);

    void activeTaskNodeProvider(TaskNodeProvider activeTaskProvider);

    void recalculateTimeEstimates();

    RoutineDataProvider routineDataProvider();
    RoutineResponse createRoutine(TaskID taskId);

    RoutineResponse startRoutineStep(StepID stepId);
    RoutineResponse pauseRoutineStep(StepID stepId);
    RoutineResponse previousRoutineStep(StepID stepId);
    RoutineResponse completeRoutineStep(StepID stepId);
    RoutineResponse skipRoutineStep(StepID stepId);
    RoutineResponse skipRoutine(RoutineID routineId);

    RoutineResponse moveRoutineStep(InsertLocation insertLocation, RoutineStep step, RoutineStep target);

    RoutineResponse setRoutineStepExcluded(StepID id, boolean exclude);
}
