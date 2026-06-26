package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamAttendanceSummary {
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
}
