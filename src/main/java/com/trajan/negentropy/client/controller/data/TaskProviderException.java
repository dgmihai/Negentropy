package com.trajan.negentropy.client.controller.data;

public class TaskProviderException extends Exception {
    public TaskProviderException(String message) {
        super(message);
    }

    public TaskProviderException(Throwable e) {
        super(e);
    }
}
