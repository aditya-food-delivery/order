package com.aditya.order_service.dto;


import com.aditya.order_service.domain.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {

    private UUID orderId;

    private BigDecimal amount;
    private OrderStatus status; // e.g. PENDING_PAYMENT
}