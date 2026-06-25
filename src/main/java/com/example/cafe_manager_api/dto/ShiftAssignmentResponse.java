package com.example.cafe_manager_api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class ShiftAssignmentResponse {
    private Integer assignmentId;
    private Integer shiftId;
    private Integer userId;
    private String fullName;
    private String role;
    private Boolean confirmed;
    private Long createdAt;

    public ShiftAssignmentResponse(Integer assignmentId, Integer shiftId, Integer userId,
                                   String fullName, String role, Boolean confirmed, Long createdAt) {
        this.assignmentId = assignmentId;
        this.shiftId = shiftId;
        this.userId = userId;
        this.fullName = fullName;
        this.role = role;
        this.confirmed = confirmed;
        this.createdAt = createdAt;
    }
}
