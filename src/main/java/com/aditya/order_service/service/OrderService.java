package com.aditya.order_service.service;



import com.aditya.contracts.event.DomainEvent;
import com.aditya.contracts.order.OrderCreatedEvent;
import com.aditya.order_service.dto.CreateOrderRequest;
import com.aditya.order_service.dto.OrderResponse;
import com.aditya.order_service.dto.OrderStatusResponse;
import com.aditya.order_service.domain.enums.OrderStatus;
import com.aditya.order_service.domain.enums.PaymentStatus;
import com.aditya.order_service.domain.model.Order;
import com.aditya.order_service.domain.model.OrderItem;
import com.aditya.order_service.event.OrderEventFactory;
import com.aditya.order_service.outbox.service.OutboxService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import com.aditya.order_service.repository.OrderRepository;

import com.aditya.order_service.outbox.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final OrderEventFactory orderEventFactory;
    private final OutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        // ✅ 0. Validate idempotency key
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        // ✅ 1. Check if order already exists (Idempotency)
        Order existing = orderRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);

        if (existing != null) {
            return mapToResponse(existing);
        }

        // 🧠 2. Calculate total
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧱 3. Build Order
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(request.getIdempotencyKey()) // 🔥 IMPORTANT
                .userId(request.getUserId())
                .totalAmount(total)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.INITIATED)
                .createdAt(Instant.now())
                .build();

        // 🧱 4. Build Order Items
        List<OrderItem> items = request.getItems().stream()
                .map(reqItem -> OrderItem.builder()
                        .id(UUID.randomUUID())
                        .menuItemId(reqItem.getMenuItemId())
                        .quantity(reqItem.getQuantity())
                        .price(reqItem.getPrice()) // ✅ price snapshot
                        .order(order)
                        .build()
                ).toList();

        order.setItems(items);

        try {
            // 💾 5. Save order
            Order saved = orderRepository.save(order);

            // 📦 6. Create event
            OrderCreatedEvent payload = OrderCreatedEvent.builder()
                    .orderId(saved.getId())
                    .userId(saved.getUserId())
                    .totalAmount(saved.getTotalAmount())
                    .build();

            DomainEvent<?> event =
                    orderEventFactory.createOrderCreatedEvent(saved.getId(), payload);

            // 📨 7. Save to outbox
            outboxService.saveEvent(
                    saved.getId(),
                    "ORDER",
                    "order.created",
                    event
            );

            return mapToResponse(saved);

        } catch (DataIntegrityViolationException ex) {
            // 🔥 CRITICAL: Handles race condition (2 parallel same requests)

            Order alreadyCreated = orderRepository
                    .findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> ex); // should exist

            return mapToResponse(alreadyCreated);
        }
    }
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .status(order.getStatus())
                .build();
    }
    @Transactional
    public void markPaymentInitiated(UUID orderId, UUID paymentId , String razorpayOrderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));



        // ✅ Valid transition check
        if (order.getStatus() != OrderStatus.CREATED) {
            return;
        }

        order.setPaymentStatus(PaymentStatus.INITIATED);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentID(paymentId);
        order.setRazorpayOrderId(razorpayOrderId);

        orderRepository.save(order);
    }

    @Transactional
    public void markOrderAsPaid(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // ✅ Idempotency
        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        // ✅ Prevent invalid transition
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.SUCCESS);

        orderRepository.save(order);
    }

    @Transactional
    public void markOrderAsFailed(UUID orderId, String reason) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // ✅ Idempotency
        if (order.getStatus() == OrderStatus.FAILED) {
            return;
        }

        // ✅ Prevent overriding success
        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        order.setStatus(OrderStatus.FAILED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setFailureReason(reason);

        orderRepository.save(order);
    }

    @Transactional
    public OrderStatusResponse getOrderStatus(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return OrderStatusResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentId(order.getPaymentID())
                .razorpayOrderId(order.getRazorpayOrderId())
                .failureReason(order.getFailureReason())
                .build();
    }




}