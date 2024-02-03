package com.trajan.negentropy.client.components.grid.enums;

public enum ColumnKey {
    DRAG_HANDLE("Drag Handle"),
    NAME("Name"),
    STATUS("Status"),
    GOTO("Go to step"),
    EXCLUDE("Excluded"),
    FOCUS("Focus"),
    COMPLETE("Complete"),
    DESCRIPTION("Description"),
    STARRED("Starred"),
    PINNED("Pinned"),
    CLEANUP("Cleanup"),
    DIFFICULT("Difficult"),
    TAGS("Tags"),
    PROJECT("Project"),
    REQUIRED("Required"),
    FROZEN("Position Frozen"),
    START_WITH_CHILDREN("Auto Start"),
    RECURRING("Recurring"),
    CYCLE_TO_END("Cycle to End"),
    CRON("Cron"),
    RESCHEDULE_NOW("Reschedule for Now"),
    RESCHEDULE_LATER("Reschedule for Later"),
    SCHEDULED_FOR("Scheduled For"),
    TAGS_COMBO("Tag Combo Box"),
    LIMIT("Limit"),
    DURATION("Single Step Duration"),
    NET_DURATION("Total Nested Duration"),
    EDIT("Edit"),
    DELETE("Delete"),
    DESCENDANTS("Descendants");

    private final String text;

    ColumnKey(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
