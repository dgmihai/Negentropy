package com.trajan.negentropy.server.broadcaster;

import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.function.Consumer;

@Slf4j
@Component
@Scope("prototype")
public class Broadcaster<T> {
    public String label = "";

    protected final LinkedList<Consumer<T>> listeners = new LinkedList<>();

    public synchronized Registration register(Consumer<T> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (Broadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public synchronized Registration registerOnce(Consumer<T> listener) {
        listeners.add(t -> {
            listener.accept(t);
            listeners.remove(listener);
        });

        return () -> {
            synchronized (Broadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public synchronized void broadcast(@Nullable T content) {
        String name = content == null ? "" : content.getClass().getSimpleName() + " ";
        log.debug(label + "Broadcasting " + name + "to " + listeners.size() + " listeners");
        for (Consumer<T> listener : listeners) {
            try {
                this.notify(listener, content);
            } catch (Exception e) {
                log.error(label + "Exception while notifying listener", e);
                listeners.remove(listener);
            }
        }
    }

    protected void notify(Consumer<T> listener, T content) {
        listener.accept(content);
    }
}
