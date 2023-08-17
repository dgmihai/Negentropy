package com.trajan.negentropy.client.components.grid;

public enum ColumnKey {
    DRAG_HANDLE("Drag Handle"),
    NAME("Name"),
    STATUS("Status"),
    GOTO("Go to step"),
    EXCLUDE("Exclude/Unexclude Task"),
    FOCUS("Focus"),
    PROJECT("Project"),
    REQUIRED("Required"),
    COMPLETE("Complete"),
    RECURRING("Recurring"),
    CRON("Cron"),
    SCHEDULED_FOR("Scheduled For"),
    TAGS("Tags"),
    DESCRIPTION("Description"),
    DURATION("Single Step Duration"),
    PROJECT_DURATION_LIMIT("Project Duration Limit"),
    TIME_ESTIMATE("Total Nested Duration"),
    EDIT("Edit"),
    DELETE("Delete");

    private final String text;

    ColumnKey(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
