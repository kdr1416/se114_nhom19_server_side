package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SchedulingRequest {
    @NotNull(message = "startDate is required")
    private Long startDate;  // epoch millis

    @NotNull(message = "endDate is required")
    private Long endDate;    // epoch millis
}
