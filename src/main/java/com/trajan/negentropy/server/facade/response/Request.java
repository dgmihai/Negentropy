package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.sync.Change;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class Request {
    private final SyncID syncId;
    protected final Collection<Change> changes;

    public Request(Collection<Change> changes) {
        this.syncId = null;
        this.changes = changes;
    }

    public void add(Change change) {
        changes.add(change);
    }

    public static Request of(Change... changes) {
        return new Request(List.of(changes));
    }

    public static Request of(SyncID syncId, Change... changes) {
        return new Request(syncId, List.of(changes));
    }

    public static Request of(SyncID syncId, Collection<Change> changes) {
        return new Request(syncId, changes);
    }
}