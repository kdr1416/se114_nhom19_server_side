package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNewsRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
    private String content;

    @NotBlank(message = "Loại thông báo không được để trống")
    private String type;

    private String priority;

    @NotBlank(message = "Đối tượng mục tiêu không được để trống")
    private String targetType;

    private String targetRole;
    private Integer targetShiftId;
    private boolean isPinned = false;
}