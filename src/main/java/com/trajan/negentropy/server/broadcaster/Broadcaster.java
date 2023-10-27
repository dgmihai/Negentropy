package com.trajan.negentropy.server.broadcaster;

import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope("prototype")
public class Broadcaster<T> {
    protected final LinkedList<Consumer<T>> listeners = new LinkedList<>();

    public synchronized Registration register(Consumer<T> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (Broadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public synchronized void broadcast(T content) {
        log.debug("Broadcasting " + content.getClass().getSimpleName() + " to " + listeners.size() + " listeners");
        for (Consumer<T> listener : listeners) {
            try {
                this.notify(listener, content);
            } catch (Exception e) {
                log.error("Exception while notifying listener", e);
                listeners.remove(listener);
            }
        }
    }

    protected void notify(Consumer<T> listener, T content) {
        listener.accept(content);
    }
}
