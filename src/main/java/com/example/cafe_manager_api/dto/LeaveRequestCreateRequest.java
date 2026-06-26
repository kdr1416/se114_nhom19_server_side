package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveRequestCreateRequest {

    @NotNull(message = "startAt is required")
    private Long startAt;

    @NotNull(message = "endAt is required")
    private Long endAt;

    @NotBlank(message = "reason is required")
    private String reason;
}
