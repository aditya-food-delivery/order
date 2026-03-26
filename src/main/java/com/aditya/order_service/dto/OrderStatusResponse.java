package com.aditya.order_service.dto;


import com.aditya.order_service.domain.enums.OrderStatus;
import com.aditya.order_service.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OrderStatusResponse {

    private UUID orderId;

    private OrderStatus status;
    private PaymentStatus paymentStatus;

    private UUID paymentId;
    private String razorpayOrderId;

    private String failureReason;
}