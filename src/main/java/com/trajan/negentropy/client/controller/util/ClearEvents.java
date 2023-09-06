package com.trajan.negentropy.client.controller.util;

import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;

public interface ClearEvents {

    List<Runnable> beforeClearCallbacks = new ArrayList<>();
    List<Runnable> afterClearCallbacks = new ArrayList<>();

    default void onClear() {}

    default void clear() {
        beforeClearCallbacks.forEach(Runnable::run);
        onClear();
        afterClearCallbacks.forEach(Runnable::run);
    }

    default Registration beforeClear(Runnable callback) {
        beforeClearCallbacks.add(callback);
        return () -> beforeClearCallbacks.remove(callback);
    }

    default Registration afterClear(Runnable callback) {
        afterClearCallbacks.add(callback);
        return () -> afterClearCallbacks.remove(callback);
    }
}
