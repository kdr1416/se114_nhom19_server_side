package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "Full name cannot be blank")
    private String fullName;

    private String phone;

    @NotBlank(message = "Role cannot be blank")
    private String role;
}
