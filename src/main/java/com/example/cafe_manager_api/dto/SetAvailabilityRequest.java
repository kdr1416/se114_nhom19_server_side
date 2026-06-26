package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetAvailabilityRequest {
    @NotNull(message = "templateId is required")
    private Integer templateId;

    @NotNull(message = "dayOfWeek is required")
    @Min(value = 1, message = "dayOfWeek must be between 1 and 7")
    @Max(value = 7, message = "dayOfWeek must be between 1 and 7")
    private Integer dayOfWeek;

    @NotNull(message = "isAvailable is required")
    private Boolean isAvailable;
}
