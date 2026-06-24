package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    private String categoryName;

    private String description;

    private Boolean isActive;
}
