package com.aditya.order_service.repository;



import com.aditya.order_service.dto.OrderStatusResponse;
import com.aditya.order_service.domain.enums.OrderStatus;
import com.aditya.order_service.domain.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // 🔍 Get orders by user
    List<Order> findByUserId(UUID userId);

    // 🔍 Filter by status
    List<Order> findByStatus(OrderStatus status);

    // 🔥 IMPORTANT: Lock row (for concurrency)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    // 🔍 Optional: fetch with items (avoid lazy issues)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    @Query("""
    SELECT new com.aditya.order_service.dto.OrderStatusResponse(
        o.id, o.status, o.paymentStatus,
        o.paymentID, o.razorpayOrderId, o.failureReason
    )
    FROM Order o
    WHERE o.id = :orderId
""")
    Optional<OrderStatusResponse> findOrderStatus(UUID orderId);

    Optional<Order> findByIdempotencyKey(String key);
}