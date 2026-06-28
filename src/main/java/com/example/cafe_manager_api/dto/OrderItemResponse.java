package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Integer orderItemId;
    private Integer orderId;
    private Integer productId;
    private String productNameSnapshot;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
    private String note;
    private Integer servedQuantity;
}
