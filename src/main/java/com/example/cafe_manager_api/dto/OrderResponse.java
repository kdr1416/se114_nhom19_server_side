package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Integer orderId;
    private Integer tableId;
    private String orderCode;
    private String status;
    private Double totalAmount;
    private String note;
    private Long createdAt;
    private Long paidAt;
    private Integer createdByUserId;
    private Integer createdShiftId;
    private String createdByFullName;
}
