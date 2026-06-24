package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.*;
import com.example.cafe_manager_api.entity.*;
import com.example.cafe_manager_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatReadRepository chatReadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<ChatRoomEntity> getRoomsForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return chatRoomRepository.findRoomsByParticipantUserId(user.getUserId());
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessageHistory(Integer roomId, int page, int size, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Validate user is participant of room
        chatParticipantRepository.findByRoomIdAndUserId(roomId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this chat room"));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessageEntity> messagesPage = chatMessageRepository.findByRoomId(roomId, pageRequest);

        return messagesPage.map(this::mapToChatMessageResponse);
    }

    @Transactional
    public ChatRoomEntity createGeneralRoom(ChatRoomRequest request, String username) {
        UserEntity creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!"ADMIN".equalsIgnoreCase(creator.getRole()) && !"MANAGER".equalsIgnoreCase(creator.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin or Manager can create rooms");
        }

        long now = System.currentTimeMillis();

        ChatRoomEntity room = new ChatRoomEntity();
        room.setRoomName(request.getRoomName().trim());
        room.setRoomType("GENERAL");
        room.setTargetRole(request.getTargetRole() != null ? request.getTargetRole().trim() : null);
        room.setCreatedBy(creator.getUserId());
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        room.setIsActive(true);

        ChatRoomEntity savedRoom = chatRoomRepository.save(room);

        // Add creator as a participant
        addParticipant(savedRoom.getRoomId(), creator.getUserId(), now, creator.getRole());

        // Find users to auto-add
        List<UserEntity> targetUsers;
        if (request.getTargetRole() != null && !request.getTargetRole().trim().isEmpty()) {
            targetUsers = userRepository.findByRole(request.getTargetRole().trim());
        } else {
            targetUsers = userRepository.findAll();
        }

        for (UserEntity targetUser : targetUsers) {
            if (Boolean.TRUE.equals(targetUser.getIsActive()) && !targetUser.getUserId().equals(creator.getUserId())) {
                addParticipant(savedRoom.getRoomId(), targetUser.getUserId(), now, targetUser.getRole());
            }
        }

        return savedRoom;
    }

    @Transactional
    public ChatMessageResponse saveAndBroadcastMessage(Integer roomId, ChatMessagePayload payload, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Validate participant
        chatParticipantRepository.findByRoomIdAndUserId(roomId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this chat room"));

        long now = System.currentTimeMillis();

        // Save message
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setRoomId(roomId);
        msg.setSenderId(user.getUserId());
        msg.setContent(payload.getContent().trim());
        msg.setCreatedAt(now);
        msg.setIsDeleted(false);
        ChatMessageEntity savedMsg = chatMessageRepository.save(msg);

        // Update ChatRoom updatedAt timestamp
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found"));
        room.setUpdatedAt(now);
        chatRoomRepository.save(room);

        // Mark as read for the sender
        ChatReadEntity read = new ChatReadEntity();
        read.setMessageId(savedMsg.getMessageId());
        read.setUserId(user.getUserId());
        read.setReadAt(now);
        chatReadRepository.save(read);

        ChatMessageResponse response = mapToChatMessageResponse(savedMsg);

        // Broadcast through WebSocket STOMP
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);

        return response;
    }

    @Transactional
    public void markRoomAsRead(Integer roomId, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Validate participant
        chatParticipantRepository.findByRoomIdAndUserId(roomId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this chat room"));

        chatReadRepository.markAllAsRead(roomId, user.getUserId(), System.currentTimeMillis());
    }

    private void addParticipant(Integer roomId, Integer userId, long joinedAt, String role) {
        Optional<ChatParticipantEntity> existing = chatParticipantRepository.findByRoomIdAndUserId(roomId, userId);
        if (existing.isEmpty()) {
            ChatParticipantEntity participant = new ChatParticipantEntity();
            participant.setRoomId(roomId);
            participant.setUserId(userId);
            participant.setJoinedAt(joinedAt);
            participant.setLeftAt(null);
            participant.setRoleInRoom(role);
            chatParticipantRepository.save(participant);
        }
    }

    private ChatMessageResponse mapToChatMessageResponse(ChatMessageEntity msg) {
        String senderName = userRepository.findById(msg.getSenderId())
                .map(UserEntity::getFullName)
                .orElse("Unknown");

        return ChatMessageResponse.builder()
                .messageId(msg.getMessageId())
                .roomId(msg.getRoomId())
                .senderId(msg.getSenderId())
                .senderName(senderName)
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .isDeleted(msg.getIsDeleted())
                .build();
    }
}
