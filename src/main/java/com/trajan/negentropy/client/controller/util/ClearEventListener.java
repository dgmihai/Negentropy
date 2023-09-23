package com.trajan.negentropy.client.controller.util;

import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;

public abstract class ClearEventListener {
    List<Runnable> beforeClearCallbacks = new ArrayList<>();
    List<Runnable> afterClearCallbacks = new ArrayList<>();

    protected abstract void onClear();

    public void clear() {
        beforeClearCallbacks.forEach(Runnable::run);
        onClear();
        afterClearCallbacks.forEach(Runnable::run);
    }

    public Registration beforeClear(Runnable callback) {
        beforeClearCallbacks.add(callback);
        return () -> beforeClearCallbacks.remove(callback);
    }

    public Registration afterClear(Runnable callback) {
        afterClearCallbacks.add(callback);
        return () -> afterClearCallbacks.remove(callback);
    }
}
