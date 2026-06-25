package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, Integer> {
    List<ChatParticipantEntity> findByRoomId(Integer roomId);
    List<ChatParticipantEntity> findByUserId(Integer userId);
    java.util.Optional<ChatParticipantEntity> findByRoomIdAndUserId(Integer roomId, Integer userId);
    long countByRoomId(Integer roomId);
}

