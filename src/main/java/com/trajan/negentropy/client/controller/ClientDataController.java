package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.client.controller.data.TaskEntryDataProvider;
import com.trajan.negentropy.client.controller.data.TaskNodeData;
import com.trajan.negentropy.client.controller.data.TaskProvider;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.id.RoutineID;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.server.facade.response.TaskResponse;

public interface ClientDataController {

    TaskEntryDataProvider dataProvider();
    UserSettings settings();
    QueryService queryService();
    TaskProvider activeTaskProvider();

    TaskEntry getBaseEntry();
    void setBaseEntry(TaskEntry entry);

    void deleteNode(TaskEntry entry);

    void updateNode(TaskEntry entry);
    void updateNode(TaskNode node);
    TaskResponse updateTask(Task task);
    TaskResponse updateTask(TaskEntry entry);
    void updateEntry(TaskEntry entry);

    void moveNodeToRoot(TaskEntry entry);

    void moveNodeInto(TaskNodeData moved, TaskNodeData target);

    void moveNodeBefore(TaskNodeData moved, TaskNodeData target);
    void moveNodeAfter(TaskNodeData moved, TaskNodeData target);

    void activeTaskProvider(TaskProvider activeTaskProvider);

    Response addTaskFromProvider(TaskProvider taskProvider);

    Response addTaskFromProviderAsChild(TaskProvider taskProvider, TaskEntry parent);
    Response addTaskFromActiveProviderAsChild(TaskEntry parent);

    Response addTaskFromProviderBefore(TaskProvider taskProvider, TaskEntry after);
    Response addTaskFromActiveProviderBefore(TaskEntry after);

    Response addTaskFromProviderAfter(TaskProvider taskProvider, TaskEntry before);
    Response addTaskFromActiveProviderAfter(TaskEntry before);

    Tag createTag(Tag tag);

    void recalculateTimeEstimates();

    RoutineResponse createRoutine(TaskID taskId);

    RoutineResponse startRoutineStep(StepID stepId);
    RoutineResponse pauseRoutineStep(StepID stepId);
    RoutineResponse previousRoutineStep(StepID stepId);
    RoutineResponse completeRoutineStep(StepID stepId);
    RoutineResponse skipRoutineStep(StepID stepId);
    RoutineResponse skipRoutine(RoutineID routineId);
}
