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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cm) FROM ChatMessageEntity cm " +
           "LEFT JOIN ChatReadEntity cr ON cm.messageId = cr.messageId AND cr.userId = :userId " +
           "WHERE cm.roomId = :roomId AND cm.isDeleted = false AND cr.readId IS NULL AND cm.senderId <> :userId")
    int countUnreadByRoomAndUser(@org.springframework.data.repository.query.Param("roomId") Integer roomId, 
                                 @org.springframework.data.repository.query.Param("userId") Integer userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cm) FROM ChatMessageEntity cm " +
           "JOIN ChatParticipantEntity cp ON cm.roomId = cp.roomId AND cp.userId = :userId " +
           "LEFT JOIN ChatReadEntity cr ON cm.messageId = cr.messageId AND cr.userId = :userId " +
           "WHERE cm.senderId <> :userId AND cm.isDeleted = false AND cr.readId IS NULL")
    int countTotalUnreadByUser(@org.springframework.data.repository.query.Param("userId") Integer userId);
}
