package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRoomRequest {

    @NotBlank(message = "Room name cannot be blank")
    private String roomName;

    private String targetRole;
}
