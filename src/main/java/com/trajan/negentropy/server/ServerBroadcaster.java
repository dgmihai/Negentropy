package com.trajan.negentropy.server;

import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.server.broadcaster.AsyncBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ServerBroadcaster extends AsyncBroadcaster<SyncRecord> {
    @Override
    public void broadcast(SyncRecord content) {
        log.debug("Broadcasting sync record " + content.id() + " with " + content.changes().size() + " changes");
        super.broadcast(content);
    }
}