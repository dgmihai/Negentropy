package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertAtChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertIntoChange;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface TaskNodeProvider extends SaveEvents<DataMapResponse> {
    ClientDataController controller();
    Task getTask();
    TaskNodeInfoData<?> getNodeInfo();

    @Override
    default boolean wasSaveSuccessful(DataMapResponse result) {
        return result.success();
    }

    default TaskNode modifyNode(LinkID nodeId) {
        Task task = getTask();
        Change taskChange = task.id() != null
                ? new MergeChange<>(task)
                : new PersistChange<>(task);

        Change nodeChange = new MergeChange<>(
                new TaskNode(nodeId, getNodeInfo()));

        Supplier<DataMapResponse> trySave = () ->
                controller().requestChanges(List.of(
                        taskChange,
                        nodeChange));

        Optional<DataMapResponse> response = handleSave(trySave);

        if (response.isPresent() && response.get().success()) {
            return (TaskNode) response.get().changeRelevantDataMap().getFirst(nodeChange.id());
        } else {
            return null;
        }
    }

    private Change getTaskChange() {
        Task task = getTask();
        return task.id() != null
                ? new MergeChange<>(task)
                : new PersistChange<>(task);
    }

    private TaskNode tryChange(Change taskChange, Change referencedInsertChange) {
        Supplier<DataMapResponse> trySave = () ->
                controller().requestChanges(List.of(
                        taskChange,
                        referencedInsertChange));

        Optional<DataMapResponse> response = handleSave(trySave);

        if (response.isPresent() && response.get().success()) {
            return (TaskNode) response.get().changeRelevantDataMap().getFirst(referencedInsertChange.id());
        } else {
            return null;
        }

    }

    default TaskNode createNode(TaskOrLinkID reference, InsertLocation location) {
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

        return tryChange(taskChange, referencedInsertChange);
    }
}