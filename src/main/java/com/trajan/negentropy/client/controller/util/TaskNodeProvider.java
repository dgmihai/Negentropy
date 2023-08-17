package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface TaskNodeProvider {
    ClientDataController controller();
    Response hasValidTask();
    Task getTask();
    TaskNodeInfoData<?> getNodeInfo();
    void onSuccessfulSave(HasTaskNodeData data);
    void onFailedSave(Response response);

    default Pair<Response, Integer> saveRequest(LinkID reference, InsertLocation location) {
        Change persistChange = Change.persist(getTask());
        Change referencedInsertChange = Change.referencedInsert(
                new TaskNodeDTO(getNodeInfo()),
                reference,
                location,
                persistChange.id());

        DataMapResponse response = controller().requestChanges(List.of(
                persistChange,
                referencedInsertChange));

        if (response.success()) {
            HasTaskNodeData data = (HasTaskNodeData) response.changeRelevantDataMap().getFirst(referencedInsertChange.id());
            onSuccessfulSave(data);
        } else {
            onFailedSave(response);
        }
        return Pair.of(response, referencedInsertChange.id());
    }


    enum OnSuccessfulSave {
        CLEAR("Clear task on save"),
        PERSIST("Keep task on save"),
        KEEP_TEMPLATE("Keep only options on save");

        private final String text;

        OnSuccessfulSave(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static Optional<OnSuccessfulSave> get(String text) {
            return Arrays.stream(OnSuccessfulSave.values())
                    .filter(op -> op.text.equals(text))
                    .findFirst();
        }
    }
}