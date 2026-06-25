package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Integer> {
    List<ChatMessageEntity> findByRoomIdOrderByCreatedAtAsc(Integer roomId);
    org.springframework.data.domain.Page<ChatMessageEntity> findByRoomId(Integer roomId, org.springframework.data.domain.Pageable pageable);
    ChatMessageEntity findTopByRoomIdOrderByCreatedAtDesc(Integer roomId);
}
