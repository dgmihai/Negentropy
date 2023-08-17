package com.trajan.negentropy.server.backend.sync;

import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordDataType;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordType;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

public class SyncManagerListener {
    @PostUpdate
    public void onUpdate(AbstractEntity entity) {
        SyncManager.instance.logChange(
                ChangeRecordType.MERGE,
                getChangeRecordDataType(entity),
                entity.id());
    }

    @PreRemove
    public void onRemove(AbstractEntity entity) {
        SyncManager.instance.logChange(
                ChangeRecordType.DELETE,
                getChangeRecordDataType(entity),
                entity.id());
    }

    @PostPersist
    public void onPersist(AbstractEntity entity) {
        SyncManager.instance.logChange(
                ChangeRecordType.PERSIST,
                getChangeRecordDataType(entity),
                entity.id());
    }

    public ChangeRecordDataType getChangeRecordDataType(AbstractEntity entity) {
        if (entity instanceof TaskEntity) {
            return ChangeRecordDataType.TASK;
        } else if (entity instanceof TaskLink) {
            return ChangeRecordDataType.LINK;
        } else if (entity instanceof TagEntity) {
            return ChangeRecordDataType.TAG;
        } else {
            throw new IllegalArgumentException("Unknown changes type");
        }

    }
}