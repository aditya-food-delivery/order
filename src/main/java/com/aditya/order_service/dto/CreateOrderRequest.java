package com.aditya.order_service.dto;




import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    private UUID userId;
    private String idempotencyKey;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private UUID menuItemId;
        private Integer quantity;
        private BigDecimal price;
    }
}