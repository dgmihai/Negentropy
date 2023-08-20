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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyncManagerListener {
    @PostUpdate
    public void onUpdate(AbstractEntity entity) {
        try {
            SyncManager.instance.logChange(
                    ChangeRecordType.MERGE,
                    getChangeRecordDataType(entity),
                    entity.id());
        } catch (NullPointerException e) {
            log.error("Error logging update change", e);
        }
    }

    @PreRemove
    public void onRemove(AbstractEntity entity) {
        try {
            SyncManager.instance.logChange(
                    ChangeRecordType.DELETE,
                    getChangeRecordDataType(entity),
                    entity.id());
        } catch (NullPointerException e) {
            log.error("Error logging delete change", e);
        }
    }

    @PostPersist
    public void onPersist(AbstractEntity entity) {
        try {
            SyncManager.instance.logChange(
                    ChangeRecordType.PERSIST,
                    getChangeRecordDataType(entity),
                    entity.id());
        } catch (NullPointerException e) {
            log.error("Error logging persist change", e);
        }
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