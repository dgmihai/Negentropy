package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.server.TaskTestTemplate;

import java.util.Set;

public class ClientTestTemplate extends TaskTestTemplate {
    protected TestClientDataController controller;

    protected TaskNodeProvider taskNodeProvider = new TaskNodeProvider() {
        @Override
        public ClientDataController controller() {
            return controller;
        }

        @Override
        public Task getTask() {
            return new Task()
                    .name(TEST_TASK_NAME)
                    .tags(Set.of(TEST_TAG));
        }

        @Override
        public TaskNodeInfoData<?> getNodeInfo() {
            return new TaskNodeDTO().recurring(true);
        }
    };

    protected static final String TEST_TASK_NAME = "TestTaskName";
    protected static final Tag TEST_TAG = new Tag(null, "TestTag");

    @Override
    protected void init() {
        super.init();

        controller = new TestClientDataController(testServices);
    }
}
