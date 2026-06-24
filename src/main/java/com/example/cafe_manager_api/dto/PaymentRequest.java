package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Integer orderId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String promotionCode;

    @NotNull(message = "Amount received is required")
    @Min(value = 0, message = "Amount received must be at least 0")
    private Double amountReceived;

    private Double discountAmount;
}
