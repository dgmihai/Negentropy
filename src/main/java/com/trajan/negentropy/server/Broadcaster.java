package com.trajan.negentropy.server;

import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
@Slf4j
public class Broadcaster {
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<SyncRecord>> listeners = new LinkedList<>();

    public synchronized Registration register(
            Consumer<SyncRecord> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (Broadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public synchronized void broadcast(SyncRecord syncRecord) {
        for (Consumer<SyncRecord> listener : listeners) {
            log.debug("Broadcasting sync record " + syncRecord.id() + " with " + syncRecord.changes().size() + " changes");
            executor.execute(() -> listener.accept(syncRecord));
        }
    }
}