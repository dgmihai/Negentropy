package com.trajan.negentropy.model.entity;

public enum TimeableStatus {
    NOT_STARTED("Not Started"),
    ACTIVE("Active"),
    DESCENDANT_ACTIVE("Subtasks Active"),
    SUSPENDED("Suspended"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    EXCLUDED("Excluded"),
    POSTPONED("Postponed"),
    LIMIT_EXCEEDED("Exceeds Limit");

    private final String text;

    TimeableStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public boolean isFinishedOrExceeded() {
        return this == COMPLETED || this == EXCLUDED || this == POSTPONED || this == LIMIT_EXCEEDED;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == EXCLUDED || this == POSTPONED;
    }

    public boolean equalsAny(TimeableStatus... statuses) {
        for (TimeableStatus status : statuses) {
            if (this == status) {
                return true;
            }
        }
        return false;
    }
}