package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift_assignments", uniqueConstraints = @UniqueConstraint(columnNames = {"shift_id", "user_id"}))
public class ShiftAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer assignmentId;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "role")
    private String role;

    @Column(name = "assigned_by")
    private Integer assignedBy;

    @Column(name = "confirmed", nullable = false)
    private Boolean confirmed;

    @Column(name = "created_at")
    private Long createdAt;
}
