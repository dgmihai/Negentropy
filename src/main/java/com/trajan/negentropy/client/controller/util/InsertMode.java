package com.trajan.negentropy.client.controller.util;

import java.util.Arrays;
import java.util.Optional;

public enum InsertMode {
    MOVE("Move"),
    ADD("Add New"),
    SHALLOW_COPY("Shallow Copy"),
    DEEP_COPY("Deep Copy");

    private final String text;

    InsertMode(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static Optional<InsertMode> get(String text) {
        return Arrays.stream(InsertMode.values())
                .filter(op -> op.text.equals(text))
                .findFirst();
    }
}
