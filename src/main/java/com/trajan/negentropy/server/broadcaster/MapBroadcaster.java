package com.trajan.negentropy.server.broadcaster;

import com.google.common.collect.ArrayListMultimap;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class MapBroadcaster<K, V> {
    protected final ArrayListMultimap<K, Consumer<V>> listenerMap = ArrayListMultimap.create();

    public synchronized Registration register(K key, Consumer<V> listener) {
        listenerMap.put(key, listener);

        return () -> {
            synchronized (MapBroadcaster.class) {
                listenerMap.remove(key, listener);
            }
        };
    }

    public synchronized void broadcast(K key, V content) {
        List<Consumer<V>> results = listenerMap.get(key);
        if (!results.isEmpty()) {
            log.debug("Broadcasting " + content.getClass().getSimpleName() + " to " + results.size() + " listeners");
            for (Consumer<V> listener : results) {
                try {
                    this.notify(listener, content);
                } catch (Exception e) {
                    log.error("Exception while notifying listener", e);
                    listenerMap.remove(key, listener);
                }
            }
        }
    }

    public void notify(Consumer<V> listener, V content) {
        listener.accept(content);
    }

    public boolean contains(K key) {
        return listenerMap.containsKey(key);
    }

    public Set<K> keySet() {
        return listenerMap.keySet();
    }
}
