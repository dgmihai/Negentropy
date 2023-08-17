package com.trajan.negentropy.model.refresh;

public enum RefreshStrategy {
    NONE(0),
    LINK(1),
    TASK(2),
    PARENT_AND_CHILDREN(3),
    ALL_ANCESTORS(4);

    private final int level;

    RefreshStrategy(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}