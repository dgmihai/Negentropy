package com.trajan.negentropy.server.backend.sync;

import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.id.ID;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetDurationSyncListener {
    @PostUpdate
    @PostPersist
    public void onChange(NetDuration netDuration) {
        try {
            SyncManager.instance.logDurationChange(
                    ID.of(netDuration.task()),
                    netDuration.val());
        } catch (NullPointerException e) {
            log.error("Error logging update change", e);
        }
    }
}