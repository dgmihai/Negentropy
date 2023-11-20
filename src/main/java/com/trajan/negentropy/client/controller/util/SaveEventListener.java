package com.trajan.negentropy.client.controller.util;

import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;

public abstract class SaveEventListener<T> {

    protected List<Runnable> beforeSaveCallbacks = new ArrayList<>();
    protected List<Runnable> afterSuccessfulSaveCallbacks = new ArrayList<>();
    protected List<Runnable> afterFailedSaveCallbacks = new ArrayList<>();

    public abstract boolean isValid();

    public abstract boolean wasSaveSuccessful(T result);

    public void handleSave(T result) {
        if (wasSaveSuccessful(result)) {
            afterSuccessfulSaveCallbacks.forEach(Runnable::run);
        } else {
            afterFailedSaveCallbacks.forEach(Runnable::run);
        }
    }

    public Registration afterSuccessfulSave(Runnable callback) {
        afterSuccessfulSaveCallbacks.add(callback);
        return () -> afterSuccessfulSaveCallbacks.remove(callback);
    }

    public Registration afterFailedSave(Runnable callback) {
        afterFailedSaveCallbacks.add(callback);
        return () -> afterFailedSaveCallbacks.remove(callback);
    }

    public Registration afterSave(Runnable callback) {
        afterSuccessfulSaveCallbacks.add(callback);
        afterFailedSaveCallbacks.add(callback);
        return () -> {
            afterFailedSaveCallbacks.remove(callback);
            afterFailedSaveCallbacks.remove(callback);
        };
    }
}
