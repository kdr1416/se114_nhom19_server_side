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

import java.util.Arrays;
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
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

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

        return messagesPage.map(msg -> {
            ChatMessageResponse resp = this.mapToChatMessageResponse(msg);
            boolean isRead = msg.getSenderId().equals(user.getUserId()) ||
                    chatReadRepository.findByMessageIdAndUserId(msg.getMessageId(), user.getUserId()).isPresent();
            resp.setIsRead(isRead);
            return resp;
        });
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
        if (request.getTargetRoles() != null) {
            room.setTargetRoles(request.getTargetRoles().trim());
        }
        room.setCreatedBy(creator.getUserId());
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        room.setIsActive(true);

        ChatRoomEntity savedRoom = chatRoomRepository.save(room);

        // Add creator as a participant
        addParticipant(savedRoom.getRoomId(), creator.getUserId(), now, creator.getRole());

        // Find users to auto-add
        List<UserEntity> targetUsers;
        if (request.getTargetRoles() != null && !request.getTargetRoles().trim().isEmpty()) {
            // Parse comma-separated roles
            List<String> roles = Arrays.asList(request.getTargetRoles().split(","));
            targetUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())
                    && roles.stream().anyMatch(r -> r.trim().equalsIgnoreCase(u.getRole())))
                .collect(Collectors.toList());
        } else if (request.getTargetRole() != null && !request.getTargetRole().trim().isEmpty()) {
            targetUsers = userRepository.findByRole(request.getTargetRole().trim());
        } else {
            targetUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRoomsWithDetails(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<ChatRoomEntity> rooms = chatRoomRepository.findRoomsByParticipantUserId(user.getUserId());

        return rooms.stream().map(room -> {
            ChatMessageEntity latest = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.getRoomId());
            String lastMessage = null;
            Long lastMessageAt = null;
            if (latest != null) {
                lastMessage = latest.getContent();
                lastMessageAt = latest.getCreatedAt();
            }

            int unread = chatReadRepository.countUnreadByRoomAndUser(room.getRoomId(), user.getUserId());
            long participantCount = chatParticipantRepository.countByRoomId(room.getRoomId());

            return ChatRoomResponse.builder()
                    .roomId(room.getRoomId())
                    .roomName(room.getRoomName())
                    .roomType(room.getRoomType())
                    .shiftId(room.getShiftId())
                    .lastMessage(lastMessage)
                    .lastMessageAt(lastMessageAt)
                    .unreadCount(unread)
                    .participantCount((int) participantCount)
                    .isActive(room.getIsActive())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addUserToSystemRooms(Integer userId, String userRole) {
        // Find all SYSTEM rooms
        List<ChatRoomEntity> systemRooms = chatRoomRepository.findByRoomType("SYSTEM");

        for (ChatRoomEntity room : systemRooms) {
            String targetRoles = room.getTargetRoles();
            if (targetRoles == null || targetRoles.isEmpty()) continue;

            // Check if this user's role matches any of the room's target roles
            List<String> roles = Arrays.asList(targetRoles.split(","));
            boolean matches = roles.stream()
                .anyMatch(r -> r.trim().equalsIgnoreCase(userRole));

            if (!matches) continue;

            // Check if user is already a participant (avoid duplicate)
            boolean alreadyMember = chatParticipantRepository
                .existsByRoomIdAndUserId(room.getRoomId(), userId);
            if (alreadyMember) continue;

            // Add user to room
            ChatParticipantEntity participant = new ChatParticipantEntity();
            participant.setRoomId(room.getRoomId());
            participant.setUserId(userId);
            participant.setJoinedAt(System.currentTimeMillis());
            participant.setRoleInRoom("MEMBER");
            chatParticipantRepository.save(participant);
        }
    }

    @Transactional(readOnly = true)
    public int getTotalUnreadCount(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return chatReadRepository.countTotalUnreadByUser(user.getUserId());
    }

    @Transactional
    public ChatRoomResponse syncShiftChatRoom(Integer shiftId, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));

        Optional<ChatRoomEntity> existingRoomOpt = chatRoomRepository.findByShiftId(shiftId);
        int roomId;
        long now = System.currentTimeMillis();

        if (existingRoomOpt.isEmpty()) {
            if ("CANCELLED".equalsIgnoreCase(shift.getStatus())) {
                return null; // Don't create chat room for cancelled shifts
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ROOT);
            String dateStr = sdf.format(new java.util.Date(shift.getShiftDate()));
            String roomName = shift.getShiftName() + " - " + dateStr + " (" + shift.getStartTime() + "-" + shift.getEndTime() + ")";

            ChatRoomEntity room = new ChatRoomEntity();
            room.setRoomName(roomName);
            room.setRoomType("SHIFT");
            room.setShiftId(shiftId);
            room.setCreatedBy(shift.getOpenedBy() != null && shift.getOpenedBy() > 0 ? shift.getOpenedBy() : user.getUserId());
            room.setCreatedAt(now);
            room.setUpdatedAt(now);
            room.setIsActive(true);
            ChatRoomEntity savedRoom = chatRoomRepository.save(room);
            roomId = savedRoom.getRoomId();
        } else {
            ChatRoomEntity room = existingRoomOpt.get();
            if ("CANCELLED".equalsIgnoreCase(shift.getStatus())) {
                room.setIsActive(false);
                ChatRoomEntity savedRoom = chatRoomRepository.save(room);
                roomId = savedRoom.getRoomId();
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ROOT);
                String dateStr = sdf.format(new java.util.Date(shift.getShiftDate()));
                String expectedName = shift.getShiftName() + " - " + dateStr + " (" + shift.getStartTime() + "-" + shift.getEndTime() + ")";

                boolean changed = false;
                if (!expectedName.equals(room.getRoomName())) {
                    room.setRoomName(expectedName);
                    changed = true;
                }
                if (!Boolean.TRUE.equals(room.getIsActive())) {
                    room.setIsActive(true);
                    changed = true;
                }
                if (changed) {
                    chatRoomRepository.save(room);
                }
                roomId = room.getRoomId();
            }
        }

        // Sync participants if not cancelled
        if (!"CANCELLED".equalsIgnoreCase(shift.getStatus())) {
            List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository.findByShiftId(shiftId);
            List<ChatParticipantEntity> currentParticipants = chatParticipantRepository.findByRoomId(roomId);

            // Add missing
            for (ShiftAssignmentEntity assign : assignments) {
                boolean exists = false;
                for (ChatParticipantEntity part : currentParticipants) {
                    if (part.getUserId().equals(assign.getUserId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ChatParticipantEntity p = new ChatParticipantEntity();
                    p.setRoomId(roomId);
                    p.setUserId(assign.getUserId());
                    p.setJoinedAt(now);
                    p.setRoleInRoom("MEMBER");
                    chatParticipantRepository.save(p);
                }
            }

            // Remove extra
            for (ChatParticipantEntity part : currentParticipants) {
                boolean stillAssigned = false;
                for (ShiftAssignmentEntity assign : assignments) {
                    if (assign.getUserId().equals(part.getUserId())) {
                        stillAssigned = true;
                        break;
                    }
                }
                if (!stillAssigned) {
                    chatParticipantRepository.delete(part);
                }
            }
        }

        ChatRoomEntity room = chatRoomRepository.findById(roomId).orElseThrow();
        ChatMessageEntity latest = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.getRoomId());
        String lastMessage = latest != null ? latest.getContent() : null;
        Long lastMessageAt = latest != null ? latest.getCreatedAt() : null;
        int unread = chatReadRepository.countUnreadByRoomAndUser(room.getRoomId(), user.getUserId());
        long participantCount = chatParticipantRepository.countByRoomId(room.getRoomId());

        return ChatRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomType(room.getRoomType())
                .shiftId(room.getShiftId())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unread)
                .participantCount((int) participantCount)
                .isActive(room.getIsActive())
                .build();
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
