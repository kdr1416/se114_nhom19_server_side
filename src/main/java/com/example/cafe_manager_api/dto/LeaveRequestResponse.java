package com.example.cafe_manager_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private Long leaveRequestId;
    private Integer userId;
    private String userName;
    private Long startAt;
    private Long endAt;
    private String reason;
    private String status;
    private Integer reviewedByUserId;
    private String reviewedByName;
    private Long reviewedAt;
    private String reviewNote;
    private Long createdAt;
    private Long updatedAt;
    private Integer affectedAssignmentCount;
}
