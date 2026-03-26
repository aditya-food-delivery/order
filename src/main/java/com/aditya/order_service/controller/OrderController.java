package com.aditya.order_service.controller;

import com.aditya.order_service.dto.CreateOrderRequest;
import com.aditya.order_service.dto.OrderResponse;
import com.aditya.order_service.dto.OrderStatusResponse;
import com.aditya.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create Order API
     * - Generates referenceId internally (service layer)
     * - Triggers payment flow (via event/outbox)
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request
    ) {

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(
            @PathVariable UUID orderId
    ) {

        OrderStatusResponse response = orderService.getOrderStatus(orderId);

        return ResponseEntity.ok(response);
    }
}