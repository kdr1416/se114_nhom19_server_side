package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Table ID is required")
    private Integer tableId;

    private String note;
}
