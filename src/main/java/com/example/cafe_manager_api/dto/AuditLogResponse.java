package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Integer logId;
    private Integer userId;
    private String action;
    private String targetType;
    private String targetId;
    private String description;
    private Long createdAt;
}
