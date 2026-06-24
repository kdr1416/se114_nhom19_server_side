package com.example.cafe_manager_api.repository;

import com.example.cafe_manager_api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Integer> {
    java.util.Optional<ChatRoomEntity> findByShiftId(Integer shiftId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM ChatRoomEntity r " +
        "JOIN ChatParticipantEntity p ON r.roomId = p.roomId " +
        "WHERE p.userId = :userId AND r.isActive = true " +
        "ORDER BY r.updatedAt DESC")
    java.util.List<ChatRoomEntity> findRoomsByParticipantUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);
}
