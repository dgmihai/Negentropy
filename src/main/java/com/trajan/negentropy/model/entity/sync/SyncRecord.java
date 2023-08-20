package com.trajan.negentropy.model.entity.sync;

import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
public class SyncRecord {
    private final SyncID id;
    private final LocalDateTime timestamp;
    private final List<Change> changes;
    private final Map<TaskID, Duration> netDurationChanges;

    public SyncRecord(SyncID id, LocalDateTime timestamp, List<Change> changes, Map<TaskID, Duration> netDurationChanges) {
        this.id = id;
        this.timestamp = timestamp;
        this.changes = changes;
        this.netDurationChanges = netDurationChanges;
    }
}
