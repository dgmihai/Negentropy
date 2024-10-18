package com.trajan.negentropy.server.broadcaster;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope("prototype")
public class AsyncBroadcaster<T> extends Broadcaster<T> {

    @Override
    protected void notify(Consumer<T> listener, T content) {
        CompletableFuture.runAsync(() -> listener.accept(content));
    }
}