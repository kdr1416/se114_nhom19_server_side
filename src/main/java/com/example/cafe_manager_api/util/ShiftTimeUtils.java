package com.example.cafe_manager_api.util;

public final class ShiftTimeUtils {

    private ShiftTimeUtils() {}

    public static long getShiftStartMillis(long baseDate, String startTimeStr) {
        return baseDate + parseTimeToMillis(startTimeStr);
    }

    public static long getShiftEndMillis(long baseDate, String startTimeStr, String endTimeStr) {
        long start = getShiftStartMillis(baseDate, startTimeStr);
        long end = baseDate + parseTimeToMillis(endTimeStr);
        if (end <= start) {
            end += 24 * 3600 * 1000L;
        }
        return end;
    }

    private static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || !timeStr.contains(":")) {
            return 0;
        }
        try {
            String[] parts = timeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return (hours * 3600L + minutes * 60L) * 1000L;
        } catch (Exception e) {
            return 0;
        }
    }
}
