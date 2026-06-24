package com.example.cafe_manager_api.util;

public final class Constants {
    private Constants() { }

    // Shift status constants
    public static final String SHIFT_DRAFT = "DRAFT";
    public static final String SHIFT_PUBLISHED = "PUBLISHED";
    public static final String SHIFT_IN_PROGRESS = "IN_PROGRESS";
    public static final String SHIFT_CLOSED = "CLOSED";
    public static final String SHIFT_CANCELLED = "CANCELLED";

    // Attendance status constants
    public static final String ATTENDANCE_ABSENT = "ABSENT";
    public static final String ATTENDANCE_CHECKED_IN = "CHECKED_IN";
    public static final String ATTENDANCE_COMPLETED = "COMPLETED";
    public static final String ATTENDANCE_LATE = "LATE";
    public static final String ATTENDANCE_EARLY_LEAVE = "EARLY_LEAVE";
}
