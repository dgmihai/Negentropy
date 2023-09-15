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
    RESCHEDULE_NOW("Reschedule for Now"),
    RESCHEDULE_LATER("Reschedule for Later"),
    SCHEDULED_FOR("Scheduled For"),
    TAGS("Tags"),
    TAGS_COMBO("Tag Combo Box"),
    DESCRIPTION("Description"),
    DURATION("Single Step Duration"),
    NET_DURATION("Total Nested Duration"),
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
