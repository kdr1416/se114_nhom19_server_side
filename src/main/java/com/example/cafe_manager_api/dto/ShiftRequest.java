package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShiftRequest {

    private Integer templateId;

    @NotBlank(message = "Shift name is required")
    private String shiftName;

    @NotNull(message = "Shift date is required")
    private Long shiftDate;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "End time is required")
    private String endTime;
}
