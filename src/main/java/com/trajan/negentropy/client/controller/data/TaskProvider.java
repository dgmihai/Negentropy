package com.trajan.negentropy.client.controller.data;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.trajan.negentropy.server.facade.response.Response;

import java.util.Optional;

public interface TaskProvider {
    Response hasValidTask();
    Optional<Task> getTask() throws TaskProviderException;
    TaskNodeInfo getNodeInfo();
}
