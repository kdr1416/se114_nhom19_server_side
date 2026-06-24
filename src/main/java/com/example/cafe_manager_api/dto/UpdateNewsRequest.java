package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateNewsRequest {
    @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
    private String title;

    @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
    private String content;
    private String type;
    private String priority;
    private String targetType;
    private String targetRole;
    private Integer targetShiftId;
    private Boolean isPinned;
}