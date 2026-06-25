package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class AssignStaffRequest {
    private Integer userId;
    private String role; // Optional, defaults to user's current role
}
