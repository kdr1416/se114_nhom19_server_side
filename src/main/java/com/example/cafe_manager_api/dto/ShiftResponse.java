package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {
    private Integer shiftId;
    private Integer templateId;
    private String shiftName;
    private Long shiftDate;
    private String startTime;
    private String endTime;
    private String status;
    private Integer openedBy;
    private Long openedAt;
    private Integer closedBy;
    private Long closedAt;
    private Double openingCash;
    private Double closingCash;
    private Long createdAt;
}
