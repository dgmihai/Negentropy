package com.trajan.negentropy.client.controller.util;

import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class SaveEventListener<T> {

    List<Runnable> beforeSaveCallbacks = new ArrayList<>();
    List<Runnable> afterSuccessfulSaveCallbacks = new ArrayList<>();
    List<Runnable> afterFailedSaveCallbacks = new ArrayList<>();

    public boolean isValid() {
        return true;
    }

    public abstract boolean wasSaveSuccessful(T result);

    public Optional<T> handleSave(Supplier<T> saveOperation) {
        beforeSaveCallbacks.forEach(Runnable::run);

        T result;
        if (isValid()) {
            result = saveOperation.get();
            if (wasSaveSuccessful(result)) {
                afterSuccessfulSaveCallbacks.forEach(Runnable::run);
            } else {
                afterFailedSaveCallbacks.forEach(Runnable::run);
            }
            return Optional.of(result);
        } else {
            afterFailedSaveCallbacks.forEach(Runnable::run);
            return Optional.empty();
        }
    }

    public Registration beforeSave(Runnable callback) {
        beforeSaveCallbacks.add(callback);
        return () -> beforeSaveCallbacks.remove(callback);
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
