package com.trajan.negentropy.model.entity.sync;

import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.sync.Change;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class SyncRecord {
    private final SyncID id;
    private final LocalDateTime timestamp;
    private final List<Change> changes;

    public SyncRecord(SyncID id, LocalDateTime timestamp, List<Change> changes) {
        this.id = id;
        this.timestamp = timestamp;
        this.changes = changes;
    }
}
