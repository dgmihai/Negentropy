package com.trajan.negentropy.client.session.enums;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Optional;

@Getter
@Accessors(fluent = true)
public enum GridTiling {
    NONE("None"),
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal");

    private final String value;

    GridTiling(String value) {
        this.value = value;
    }

    public static Optional<GridTiling> get(String string) {
        return Arrays.stream(GridTiling.values())
                .filter(env -> env.value.equals(string))
                .findFirst();
    }
}
