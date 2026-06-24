package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"tables\"")
public class TableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Integer tableId;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "status")
    private String status;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "area")
    private String area;

    @Column(name = "created_at")
    private Long createdAt;
}
