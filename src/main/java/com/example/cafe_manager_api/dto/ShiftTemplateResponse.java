package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftTemplateResponse {
    private Integer templateId;
    private String templateName;
    private String startTime;
    private String endTime;
    private Integer minStaff;
    private Boolean isActive;
    private Long createdAt;
}
