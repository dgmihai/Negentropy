package com.trajan.negentropy.model.entity;

public enum TimeableStatus {
    NOT_STARTED("Not Started"),
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    EXCLUDED("Excluded"),
    POSTPONED("Postponed");

    private final String text;

    TimeableStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == EXCLUDED || this == POSTPONED;
    }
}