package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloseShiftRequest {
    @NotNull(message = "Số tiền đóng ca là bắt buộc")
    private Double closingCash;
}
