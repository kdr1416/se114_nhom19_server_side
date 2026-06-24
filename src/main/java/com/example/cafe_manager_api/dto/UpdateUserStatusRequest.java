package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "isActive status must be provided")
    private Boolean isActive;
}
