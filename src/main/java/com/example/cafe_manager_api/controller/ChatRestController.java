package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.dto.ChatRoomRequest;
import com.example.cafe_manager_api.dto.ChatMessageResponse;
import com.example.cafe_manager_api.entity.ChatRoomEntity;
import com.example.cafe_manager_api.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatRestController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomEntity>> getRooms(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
        List<ChatRoomEntity> rooms = chatService.getRoomsForUser(principal.getName());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
        Page<ChatMessageResponse> messages = chatService.getMessageHistory(roomId, page, size, principal.getName());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ChatRoomEntity> createRoom(
            @Valid @RequestBody ChatRoomRequest request, 
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
        ChatRoomEntity room = chatService.createGeneralRoom(request, principal.getName());
        return ResponseEntity.ok(room);
    }

    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Integer roomId, 
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
        }
        chatService.markRoomAsRead(roomId, principal.getName());
        return ResponseEntity.ok().build();
    }
}
