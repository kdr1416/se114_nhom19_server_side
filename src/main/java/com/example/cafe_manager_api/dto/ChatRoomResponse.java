package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private Integer roomId;
    private String roomName;
    private String roomType;
    private Integer shiftId;
    private String lastMessage;
    private Long lastMessageAt;
    private Integer unreadCount;
    private Integer participantCount;
    private Boolean isActive;
}
