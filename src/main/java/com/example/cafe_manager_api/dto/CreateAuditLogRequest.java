package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class CreateAuditLogRequest {
    private String action;
    private String targetType;
    private String targetId;
    private String description;
}
