package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertAtChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertIntoChange;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;

import java.util.ArrayList;
import java.util.List;

public abstract class TaskNodeProvider extends SaveEventListener<DataMapResponse> {
    protected UIController controller;
    protected ChangeID changeId;
    public boolean async = true;

    public abstract Task getTask();
    public abstract TaskNodeInfoData<?> getNodeInfo();

    public TaskNodeProvider(UIController controller) {
        super();
        this.controller = controller;
    }

    @Override
    public boolean wasSaveSuccessful(DataMapResponse result) {
        return result.success();
    }

    public void modifyNode(LinkID nodeId) {
        if (isValid()) {
            Task task = getTask();
            List<Change> changes = new ArrayList<>();
            if (task.id() == null) {
                changes.add(new PersistChange<>(task));
            }
            Change taskChange = task.id() != null
                    ? new MergeChange<>(task)
                    : new PersistChange<>(task);

            Change nodeChange = new MergeChange<>(
                    new TaskNode(nodeId, getNodeInfo()));
            changeId = nodeChange.id();

            controller.requestChangesAsync(List.of(
                            taskChange,
                            nodeChange),
                    this::handleSave);
        } else {
            afterFailedSaveCallbacks.forEach(Runnable::run);
        }
    }

    public void modifyTask() {
        if (isValid()) {
            Change taskChange = getTaskChange();
            changeId = taskChange.id();

            controller.requestChangeAsync(taskChange, this::handleSave);
        } else {
            afterFailedSaveCallbacks.forEach(Runnable::run);
        }
    }

    protected Change getTaskChange() {
        Task task = getTask();
        return task.id() != null
                ? new MergeChange<>(task)
                : new PersistChange<>(task);
    }

    protected void tryChange(Change taskChange, Change referencedInsertChange) {
        if (isValid()) {
            if (async) {
                controller.requestChangesAsync(List.of(
                                taskChange,
                                referencedInsertChange),
                        this::handleSave);
            } else {
                this.handleSave(controller.requestChanges(List.of(
                                taskChange,
                                referencedInsertChange)));
            }
        } else {
            afterFailedSaveCallbacks.forEach(Runnable::run);
        }
    }

    public ChangeID createNode(TaskOrLinkID reference, InsertLocation location) {
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
        changeId = referencedInsertChange.id();

        tryChange(taskChange, referencedInsertChange);
        return referencedInsertChange.id();
    }

    @Override
    public void handleSave(DataMapResponse response) {
        super.handleSave(response);
        changeId = null;
    }

    public interface HasTaskNodeProvider {
        TaskNodeProvider taskNodeProvider();
    }
}