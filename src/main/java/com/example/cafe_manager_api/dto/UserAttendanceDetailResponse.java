package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAttendanceDetailResponse {
    private Integer userId;
    private String fullName;
    private String username;
    private String role;
    private Integer totalShifts;
    private Integer attendedShifts;
    private Integer absentShifts;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Double totalHoursWorked;
    private Double attendanceRate;
    private Integer ordersCreated;
    private Integer paymentsProcessed;
    private Double revenueProcessed;
    private List<AttendanceRecord> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecord {
        private Integer shiftId;
        private String shiftName;
        private Long shiftDate;
        private String startTime;
        private String endTime;
        private Double durationHours;
        private Long checkInAt;
        private Long checkOutAt;
        private Integer lateMinutes;
        private Integer earlyLeaveMinutes;
        private String status; // "COMPLETED", "LATE", "EARLY_LEAVE", "ABSENT", "NOT_CHECKED_IN", "IN_PROGRESS", "UPCOMING"
        private String notes;
        private Integer ordersCreated;
        private Integer paymentsProcessed;
        private Double revenueProcessed;
    }
}
