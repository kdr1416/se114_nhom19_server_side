package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.entity.ChatParticipantEntity;
import com.example.cafe_manager_api.entity.ChatRoomEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.ChatParticipantRepository;
import com.example.cafe_manager_api.repository.ChatRoomRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatInitializationService {

    private static final Logger log = LoggerFactory.getLogger(ChatInitializationService.class);

    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatParticipantRepository chatParticipantRepository;
    @Autowired private UserRepository userRepository;

    // 3 system rooms definition
    private static final List<SystemRoomDef> SYSTEM_ROOMS = List.of(
        new SystemRoomDef("Toàn bộ nhân viên",     "STAFF,MANAGER,ADMIN"),
        new SystemRoomDef("Nhân viên & Quản lý",   "STAFF,MANAGER"),
        new SystemRoomDef("Quản lý & Admin",        "MANAGER,ADMIN")
    );

    // Simple inner record/class for room definition
    private static class SystemRoomDef {
        final String name;
        final String targetRoles;
        SystemRoomDef(String name, String targetRoles) {
            this.name = name;
            this.targetRoles = targetRoles;
        }
    }

    @PostConstruct
    public void initializeSystemRooms() {
        log.info("ChatInitializationService: checking system rooms...");

        for (SystemRoomDef def : SYSTEM_ROOMS) {
            // Check if room already exists by name AND roomType=SYSTEM
            Optional<ChatRoomEntity> existing =
                chatRoomRepository.findByRoomNameAndRoomType(def.name, "SYSTEM");

            ChatRoomEntity room;
            if (existing.isEmpty()) {
                // Create new system room
                room = new ChatRoomEntity();
                room.setRoomName(def.name);
                room.setRoomType("SYSTEM");
                room.setTargetRoles(def.targetRoles);
                room.setCreatedBy(null); // system-created, no owner
                room.setCreatedAt(System.currentTimeMillis());
                room.setUpdatedAt(System.currentTimeMillis());
                room.setIsActive(true);
                room = chatRoomRepository.save(room);
                log.info("Created system room: {}", def.name);
            } else {
                room = existing.get();
                // Update targetRoles in case it changed
                room.setTargetRoles(def.targetRoles);
                chatRoomRepository.save(room);
            }

            // Sync participants: add all active users matching targetRoles
            syncRoomParticipants(room);
        }

        log.info("ChatInitializationService: system rooms initialized.");
    }

    private void syncRoomParticipants(ChatRoomEntity room) {
        List<String> roles = Arrays.asList(room.getTargetRoles().split(","));
        List<UserEntity> eligibleUsers = userRepository.findAll().stream()
            .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
            .filter(u -> roles.stream()
                .anyMatch(r -> r.trim().equalsIgnoreCase(u.getRole())))
            .collect(Collectors.toList());

        for (UserEntity user : eligibleUsers) {
            boolean alreadyMember = chatParticipantRepository
                .existsByRoomIdAndUserId(room.getRoomId(), user.getUserId());
            if (alreadyMember) continue;

            ChatParticipantEntity p = new ChatParticipantEntity();
            p.setRoomId(room.getRoomId());
            p.setUserId(user.getUserId());
            p.setJoinedAt(System.currentTimeMillis());
            p.setRoleInRoom("MEMBER");
            chatParticipantRepository.save(p);
        }
    }
}
