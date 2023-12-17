package com.trajan.negentropy.client.components.grid.enums;

public enum ColumnKey {
    DRAG_HANDLE("Drag Handle"),
    NAME("Name"),
    STATUS("Status"),
    GOTO("Go to step"),
    EXCLUDE("Exclude/Unexclude Task"),
    FOCUS("Focus"),
    COMPLETE("Complete"),
    DESCRIPTION("Description"),
    STARRED("Starred"),
    DIFFICULT("Difficult"),
    TAGS("Tags"),
    PROJECT("Project"),
    REQUIRED("Required"),
    FROZEN("Position Frozen"),
    RECURRING("Recurring"),
    CYCLE_TO_END("Cycle to End"),
    CRON("Cron"),
    RESCHEDULE_NOW("Reschedule for Now"),
    RESCHEDULE_LATER("Reschedule for Later"),
    SCHEDULED_FOR("Scheduled For"),
    TAGS_COMBO("Tag Combo Box"),
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
