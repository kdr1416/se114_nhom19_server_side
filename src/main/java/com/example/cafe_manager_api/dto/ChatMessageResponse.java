package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Integer messageId;
    private Integer roomId;
    private Integer senderId;
    private String senderName;
    private String content;
    private Long createdAt;
    private Boolean isDeleted;
}
