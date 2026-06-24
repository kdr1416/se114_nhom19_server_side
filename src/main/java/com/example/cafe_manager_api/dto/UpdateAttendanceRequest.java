package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class UpdateAttendanceRequest {
    private String status;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private String notes;
}
