package com.trajan.negentropy.client.controller.util;

import java.util.Arrays;
import java.util.Optional;

public enum OnSuccessfulSaveActions {
    CLOSE("Close window on save"),
    CLEAR("Clear task on save"),
    PERSIST("Keep task on save"),
    KEEP_TEMPLATE("Keep only options on save");

    private final String text;

    OnSuccessfulSaveActions(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static Optional<OnSuccessfulSaveActions> get(String text) {
        return Arrays.stream(OnSuccessfulSaveActions.values())
                .filter(op -> op.text.equals(text))
                .findFirst();
    }
}