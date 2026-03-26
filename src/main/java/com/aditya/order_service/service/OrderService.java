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
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import com.aditya.order_service.repository.OrderRepository;

import com.aditya.order_service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        // 🧠 1. Calculate total
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧱 2. Build Order
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .totalAmount(total)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.INITIATED)
                .createdAt(Instant.now())
                .build();

        // 🧱 3. Build Order Items
        List<OrderItem> items = request.getItems().stream()
                .map(reqItem -> OrderItem.builder()
                        .id(UUID.randomUUID())
                        .menuItemId(reqItem.getMenuItemId())
                        .quantity(reqItem.getQuantity())
                        .price(reqItem.getPrice())
                        .order(order)
                        .build()
                ).collect(Collectors.toList());

        order.setItems(items);
        //save order
         Order saved = orderRepository.save(order);

        OrderCreatedEvent payload = OrderCreatedEvent.builder()
                 .orderId(saved.getId())
                .userId(saved.getUserId())
                .totalAmount(saved.getTotalAmount()).build();


        DomainEvent<?> event =
                orderEventFactory.createOrderCreatedEvent(saved.getId(),payload);


        outboxService.saveEvent(
                saved.getId(),
"ORDER",
                "order.created",
                event
        );


        return OrderResponse.builder()
                .orderId(saved.getId())
                .amount(saved.getTotalAmount())
                .status(saved.getStatus()).build();
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
        if (order.getStatus() == OrderStatus.FAILED) {
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