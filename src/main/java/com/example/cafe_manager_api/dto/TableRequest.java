package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TableRequest {

    @NotBlank(message = "Table name cannot be blank")
    private String tableName;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotBlank(message = "Area cannot be blank")
    private String area;
}
