package com.example.cafe_manager_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingResponse {
    private Long runId;
    private Long startDate;
    private Long endDate;
    private String status;
    private Long createdAt;
    private Long appliedAt;  // nullable
    private List<ShiftSuggestion> suggestions;
    private Integer totalShifts;
    private Integer fulfilledShifts;
    private Integer missingShifts;
}
