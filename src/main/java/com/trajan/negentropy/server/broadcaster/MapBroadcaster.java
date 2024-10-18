package com.trajan.negentropy.server.broadcaster;

import com.google.common.collect.ArrayListMultimap;
import com.trajan.negentropy.client.logger.UILogger;
import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MapBroadcaster<K, V> {
    private final UILogger log = new UILogger();

    public String label = "";

    protected final ArrayListMultimap<K, Consumer<V>> listenerMap = ArrayListMultimap.create();
    protected final ArrayList<Consumer<V>> massListeners = new ArrayList<>();

    public synchronized Registration register(K key, Consumer<V> listener, Runnable remove) {
        listenerMap.put(key, listener);

        return () -> {
            synchronized (MapBroadcaster.class) {
                listenerMap.remove(key, listener);
                remove.run();
            }
        };
    }

    public synchronized Registration register(K key, Consumer<V> listener) {
        return register(key, listener, () -> {});
    }

    public synchronized Registration register(Consumer<V> listener) {
        massListeners.add(listener);

        return () -> {
            synchronized (MapBroadcaster.class) {
                massListeners.remove(listener);
            }
        };
    }

    public synchronized void broadcast(K key, V content) {
        List<Consumer<V>> results = List.copyOf(listenerMap.get(key));
        if (!results.isEmpty()) {
            log.debug(label + "Broadcasting " + content.getClass().getSimpleName() + " to " + results.size() + " listeners");
            for (Consumer<V> listener : results) {
                try {
                    this.notify(listener, content);
                } catch (Exception e) {
                    log.error(label + "Exception while notifying listener", e);
                    listenerMap.remove(key, listener);
                }
            }
        }

        massListeners.forEach(listener -> this.notify(listener, content));
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
