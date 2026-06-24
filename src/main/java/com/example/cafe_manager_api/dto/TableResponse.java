package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableResponse {
    private Integer tableId;
    private String tableName;
    private String status;
    private Integer capacity;
    private String area;
    private Long createdAt;
}
