package com.trajan.negentropy.client;

public class K {
    public static final String INLINE_ICON_SIZE = "16px";
    public static final String OK = "OK";

    public static final String COLOR_PRIMARY = "primary-color";
    public static final String COLOR_ERROR = "error-color";
    public static final String COLOR_UNSELECTED = "unselected-color";
    public static final String COLOR_GRAYED = "grayed-color";

    public static final String ICON_COLOR_PRIMARY = "primary-color-icon";
    public static final String ICON_COLOR_UNSELECTED = "unselected-color-icon";
    public static final String ICON_COLOR_COMPLEMENTARY = "complementary-color-icon";
    public static final String ICON_COLOR_ERROR = "error-color-icon";
    public static final String ICON_COLOR_GRAYED = "grayed-color-icon";
    public static final String ICON_COLOR_BRIGHT = "bright-color-icon";


    public static final String QUICK_CREATE = "Quick Create";

    public static final String GRID_PARTNAME_REQUIRED = "required-task";
    public static final String GRID_PARTNAME_COMPLETED = "completed-node";
    public static final String GRID_PARTNAME_RECURRING = "recurring-node";
    public static final String GRID_PARTNAME_PROJECT = "project-task";
    public static final String GRID_PARTNAME_DIFFICULT = "difficult-task";
    public static final String GRID_PARTNAME_DURATION_LIMIT_EXCEEDED = "exceeds-duration-limit";

    public static final int BREAKPOINT_PX = 600;

    public static final String SHORTENED_CRON_PLACEHOLDER = "-- -- -- --";
    public static final String DURATION_PLACEHOLDER = "-- -- --";

    public static final String CRON_PATTERN =
            "^(?![-/,])[0-9*/,-]+(?<![-/,])\\s(?![-/,])[0-9*/,-]+(?<![-/,])\\s(?![-/,])[0-9*/,-]+(?<![-/,])\\s(?![-/,])[?0-9*/LW,-]+(?<![-/,])\\s(?![-/,])[0-9*/JANFEBMARAPRMAYJUNJULAUGSEPOCTNOVDEC,-]+(?<![-/,])\\s(?![-/,])[?L#0-9*/MONTUEWEDTHUFRISATSUN,-]+(?<![-/,])$";
    public static final String CRON_SHORT_PATTERN =
            "^(?![-/,])[0-9*/,-]+(?<![-/,])\\s(?![-/,])[?0-9*/LW,-]+(?<![-/,])\\s(?![-/,])[0-9*/JANFEBMARAPRMAYJUNJULAUGSEPOCTNOVDEC,-]+(?<![-/,])\\s(?![-/,])[?L#0-9*/MONTUEWEDTHUFRISATSUN,-]+(?<![-/,])$";

    public static final String COLUMN_ID_DRAG_HANDLE = "drag-handle-column";
    public static final String SHORT_SCREEN_WIDTH = "600px";
    public static final String MEDIUM_SCREEN_WIDTH = "1000px";
    public static final String NULL_CRON = "0 0 0 30 2 ?";
}