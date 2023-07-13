package com.trajan.negentropy.client.controller.data;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;

public interface TaskNodeData extends HasTaskData {
    default Task task() {
        return node().child();
    }

    TaskNode node();
}
