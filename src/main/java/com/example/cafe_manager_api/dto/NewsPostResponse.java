package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsPostResponse {
    private Integer postId;
    private String title;
    private String content;
    private String type;
    private String priority;
    private String targetType;
    private String targetRole;
    private Integer targetShiftId;
    private Integer createdByUserId;
    private String authorName;
    private Long createdAt;
    private Long updatedAt;
    private Boolean isPinned;
    private Boolean isDeleted;
    private Boolean isRead;
}