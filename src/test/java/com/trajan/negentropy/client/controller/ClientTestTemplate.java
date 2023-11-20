package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertAtChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertIntoChange;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientTestTemplate extends TaskTestTemplate {
    protected TestUIController controller;

    protected TestTaskNodeProvider taskNodeProvider;

    protected static final String TEST_TASK_NAME = "TestTaskName";
    protected static final Tag TEST_TAG = new Tag(null, "TestTag");

    void assertTaskInserted(int position, String parent, TaskNode resultNode) {
        TaskLink resultLink = entityQueryService.getLink(resultNode.linkId());
        assertEquals(TEST_TASK_NAME, resultLink.child().name());
        assertEquals(TEST_TAG.name(), queryService.fetchTags(ID.of(resultLink.child()))
                .findFirst().get().name());
        assertEquals(parent,
                resultLink.parent() != null
                        ? resultLink.parent().name()
                        : null);
        assertEquals(position, resultLink.position());
    }

    @Override
    protected void init() {
        super.init();

        controller = new TestUIController(testServices);
        taskNodeProvider = new TestTaskNodeProvider(controller);
    }

    public static class TestTaskNodeProvider extends TaskNodeProvider {
        public TestTaskNodeProvider(UIController controller) {
            super(controller);
        }

        @Override
        public TaskDTO getTask() {
            TaskDTO task = new TaskDTO();
            task.tags(Set.of(TEST_TAG)).name(TEST_TASK_NAME);
            return task;
        }

        @Override
        public TaskNodeInfoData<?> getNodeInfo() {
            return new TaskNodeDTO().recurring(true);
        }

        public TaskNode createNode_TEST(TaskOrLinkID reference, InsertLocation location) {
            if (isValid()) {
                Change taskChange = getTaskChange();

                Change referencedInsertChange;
                if (reference == null) {
                    referencedInsertChange = new ReferencedInsertIntoChange(
                            new TaskNodeDTO(getNodeInfo()),
                            null,
                            location,
                            taskChange.id());
                } else if (reference instanceof TaskID taskId) {
                    referencedInsertChange = new ReferencedInsertIntoChange(
                            new TaskNodeDTO(getNodeInfo()),
                            taskId,
                            location,
                            taskChange.id());
                } else if (reference instanceof LinkID linkId) {
                    referencedInsertChange = new ReferencedInsertAtChange(
                            new TaskNodeDTO(getNodeInfo()),
                            linkId,
                            location,
                            taskChange.id());
                } else  {
                    throw new IllegalArgumentException("Invalid reference type: " + reference.getClass());
                }

                DataMapResponse response = controller.requestChanges(List.of(
                        taskChange,
                        referencedInsertChange));
                handleSave(response);

                if (response.success()) {
                    return (TaskNode) response.changeRelevantDataMap().getFirst(referencedInsertChange.id());
                }
            }
            afterFailedSaveCallbacks.forEach(Runnable::run);
            return null;
        }

        public Task modifyTask_TEST(TaskID taskId) {
            if (isValid()) {
                Change taskChange = getTaskChange();

                DataMapResponse response = controller.requestChange(taskChange);
                handleSave(response);

                if (response.success()) {
                    return (Task) response.changeRelevantDataMap().getFirst(taskChange.id());
                }
            }
            afterFailedSaveCallbacks.forEach(Runnable::run);
            return null;
        }

        public TaskNode modifyNode_TEST(LinkID nodeId) {
            if (isValid()) {
                Task task = getTask();
                Change taskChange = task.id() != null
                        ? new MergeChange<>(task)
                        : new PersistChange<>(task);

                Change nodeChange = new MergeChange<>(
                        new TaskNode(nodeId, getNodeInfo()));

                DataMapResponse response = controller.requestChanges(List.of(
                        taskChange,
                        nodeChange));
                handleSave(response);

                if (response.success()) {
                    return (TaskNode) response.changeRelevantDataMap().getFirst(nodeChange.id());
                }
            }
            afterFailedSaveCallbacks.forEach(Runnable::run);
            return null;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
