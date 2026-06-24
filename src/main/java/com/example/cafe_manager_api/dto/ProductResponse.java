package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Integer productId;
    private Integer categoryId;
    private String productName;
    private Double price;
    private String imageUrl;
    private Boolean isActive;
    private Long createdAt;
}
