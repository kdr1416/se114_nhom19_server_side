package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "areas")
public class AreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "prefix")
    private String prefix;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private Long createdAt;
}
