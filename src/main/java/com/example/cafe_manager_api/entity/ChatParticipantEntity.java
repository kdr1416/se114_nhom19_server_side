package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class ChatParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Integer participantId;

    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "joined_at")
    private Long joinedAt;

    @Column(name = "left_at")
    private Long leftAt;

    @Column(name = "role_in_room")
    private String roleInRoom;
}
