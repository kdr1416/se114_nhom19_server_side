package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift_templates")
public class ShiftTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "min_staff")
    private Integer minStaff;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "effective_from_date")
    private Long effectiveFromDate;  // nullable - epoch millis

    @Column(name = "effective_to_date")
    private Long effectiveToDate;    // nullable - epoch millis, null = indefinite

    @Column(name = "created_at")
    private Long createdAt;
}
