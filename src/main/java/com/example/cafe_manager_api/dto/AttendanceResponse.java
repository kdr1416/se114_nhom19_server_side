package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Integer attendanceId;
    private Integer shiftId;
    private Integer userId;
    private String username;
    private Long checkInAt;
    private Long checkOutAt;
    private String status;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private String notes;
}
