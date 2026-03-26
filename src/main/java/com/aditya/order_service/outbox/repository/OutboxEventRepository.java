package com.aditya.order_service.outbox.repository;

import com.aditya.order_service.outbox.model.OutboxEvent;
import com.aditya.order_service.outbox.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
    @Query("""
SELECT e FROM OutboxEvent e
WHERE
  (e.status = 'PENDING')
  OR
  (e.status = 'FAILED' AND e.nextRetryAt <= CURRENT_TIMESTAMP)
ORDER BY e.createdAt ASC
""")
    List<OutboxEvent> findProcessableEvents();
}