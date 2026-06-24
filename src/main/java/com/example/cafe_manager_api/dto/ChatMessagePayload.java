package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessagePayload {

    @NotBlank(message = "Message content cannot be blank")
    private String content;
}
