package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.ChatMessagePayload;
import com.example.cafe_manager_api.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat/{roomId}/send")
    public void handleSendMessage(
            @DestinationVariable Integer roomId,
            ChatMessagePayload payload,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            // Silently drop messages from unauthenticated websocket clients
            return;
        }
        
        chatService.saveAndBroadcastMessage(roomId, payload, principal.getName());
    }
}
