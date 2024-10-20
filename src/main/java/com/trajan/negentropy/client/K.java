package com.trajan.negentropy.client;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

public class K {
    public static final String INLINE_ICON_SIZE = "16px";
    public static final String OK = "OK";

    public static final String COLOR_PRIMARY = "primary-color";
    public static final String COLOR_ERROR = "error-color";
    public static final String COLOR_UNSELECTED = "unselected-color";
    public static final String COLOR_GRAYED = "grayed-color";
    public static final String COLOR_TRANSPARENT = "transparent-color";

    public static final String ICON_COLOR_PRIMARY = "primary-color-icon";
    public static final String ICON_COLOR_UNSELECTED = "unselected-color-icon";
    public static final String ICON_COLOR_COMPLEMENTARY = "complementary-color-icon";
    public static final String ICON_COLOR_ERROR = "error-color-icon";
    public static final String ICON_COLOR_GRAYED = "grayed-color-icon";
    public static final String ICON_COLOR_BRIGHT = "bright-color-icon";

    public static final String BACKGROUND_COLOR_PRIMARY = "primary-color-10pct-background";

    public static final String TOGGLEABLE_BUTTON_TRUE = "toggleable-button-true";
    public static final String TOGGLEABLE_BUTTON_FALSE = "toggleable-button-false";
    public static final String TOGGLEABLE_BUTTON_NULL = "toggleable-button-inactive";

    public static final String QUICK_CREATE = "Quick Create";

    public static final String GRID_PARTNAME_FUTURE = "future-task";
    public static final String GRID_PARTNAME_COMPLETED = "completed-node";
    public static final String GRID_PARTNAME_PRIMARY = "primary-color";
    public static final String GRID_PARTNAME_PROJECT = "project-task";
    public static final String GRID_PARTNAME_DIFFICULT = "difficult-task";
    public static final String GRID_PARTNAME_DURATION_LIMIT_EXCEEDED = "exceeds-duration-limit";
    public static final String GRID_PARTNAME_ACTIVE_ROUTINE_STEP = "active-routine-step-task";

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
    public static final CronExpression NULL_CRON_FULL = CronExpression.parse("0 0 0 30 2 ?");
    public static final String NULL_CRON_SHORT = "0 30 2 ?";

    public static final LocalDateTime EPOCH_DATE = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
}