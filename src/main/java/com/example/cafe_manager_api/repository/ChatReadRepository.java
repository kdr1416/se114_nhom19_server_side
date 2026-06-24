package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatReadRepository extends JpaRepository<ChatReadEntity, Integer> {
    Optional<ChatReadEntity> findByMessageIdAndUserId(Integer messageId, Integer userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO chat_reads (message_id, user_id, read_at) " +
                   "SELECT cm.message_id, :userId, :now " +
                   "FROM chat_messages cm " +
                   "WHERE cm.room_id = :roomId AND cm.sender_id <> :userId " +
                   "ON CONFLICT (message_id, user_id) DO NOTHING", nativeQuery = true)
    void markAllAsRead(@org.springframework.data.repository.query.Param("roomId") Integer roomId, 
                       @org.springframework.data.repository.query.Param("userId") Integer userId, 
                       @org.springframework.data.repository.query.Param("now") Long now);
}
