package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {

    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "Product name cannot be blank")
    private String productName;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be less than 0")
    private Double price;

    private String imageUrl;

    private Boolean isActive;
}
