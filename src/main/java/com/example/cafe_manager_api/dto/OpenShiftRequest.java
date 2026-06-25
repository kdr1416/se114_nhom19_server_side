package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenShiftRequest {
    @NotNull(message = "Số tiền mở ca là bắt buộc")
    private Double openingCash;
}
