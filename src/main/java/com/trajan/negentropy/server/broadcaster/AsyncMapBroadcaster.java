package com.trajan.negentropy.server.broadcaster;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope("prototype")
public class AsyncMapBroadcaster<K, V> extends MapBroadcaster<K, V> {
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void notify(Consumer<V> listener, V content) {
        executor.execute(() -> listener.accept(content));
    }
}
