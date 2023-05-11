package com.trajan.negentropy.client.util;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.response.Response;

import java.util.Optional;

public interface TaskProvider {
    Response hasValidTask();
    Optional<Task> getTask() throws TaskProviderException;
}
