package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShiftTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "End time is required")
    private String endTime;

    @NotNull(message = "Minimum staff is required")
    @Min(value = 1, message = "Minimum staff must be at least 1")
    private Integer minStaff;

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
