package com.trajan.negentropy.server.broadcaster;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class AsyncBroadcaster<T> extends Broadcaster<T> {
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void notify(Consumer<T> listener, T content) {
        executor.execute(() -> listener.accept(content));
    }
}