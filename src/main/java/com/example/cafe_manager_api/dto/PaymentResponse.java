package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Integer paymentId;
    private Double subtotal;
    private Double discountAmount;
    private Double total;
    private Double change;
    private Integer cashierUserId;
    private String cashierFullName;
    private String paymentMethod;
    private Long paidAt;
}
