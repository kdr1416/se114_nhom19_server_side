package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAreaRequest {
    @NotBlank(message = "Tên khu vực không được để trống")
    private String areaName;

    @NotBlank(message = "Tiền tố không được để trống")
    private String prefix;

    private String description;
}