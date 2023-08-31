package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertChange;
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

    default TaskNode createNode(LinkID reference, InsertLocation location) {
        Task task = getTask();
        Change taskChange = task.id() != null
                ? new MergeChange<>(task)
                : new PersistChange<>(task);

        Change referencedInsertChange = new ReferencedInsertChange(
                new TaskNodeDTO(getNodeInfo()),
                reference,
                location,
                taskChange.id());

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
}