package com.aditya.order_service.event.consumer;


import com.aditya.order_service.config.RabbitMQConfig;
import com.aditya.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.aditya.contracts.payment.PaymentCompletedEvent;
import com.aditya.contracts.payment.PaymentFailedEvent;
import com.aditya.contracts.payment.PaymentInitiatedEvent;
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;

    // 🔹 1. Payment Initiated
    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_INITIATED_QUEUE
    )
    public void handlePaymentInitiated(PaymentInitiatedEvent event) {

        log.info("Received PaymentInitiatedEvent for orderId={}", event.getOrderId());
        log.info(event.getGatewayOrderId());
        log.info(event.getAmount().toString());

        try {
            orderService.markPaymentInitiated(
                    event.getOrderId(),
                    event.getPaymentId(),
                    event.getGatewayOrderId()
            );
        } catch (Exception e) {
            log.error("Error processing PaymentInitiatedEvent for orderId={}", event.getOrderId(), e);
            throw e; // 🔥 IMPORTANT → triggers retry/DLQ later
        }
    }

    // 🔹 2. Payment Completed
    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {

        log.info("Received PaymentCompletedEvent for orderId={}", event.getOrderId());

        try {
            orderService.markOrderAsPaid(event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing PaymentCompletedEvent for orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    // 🔹 3. Payment Failed
    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {

        log.info("Received PaymentFailedEvent for orderId={}", event.getOrderId());

        try {
            orderService.markOrderAsFailed(
                    event.getOrderId(),
                    event.getReason()
            );
        } catch (Exception e) {
            log.error("Error processing PaymentFailedEvent for orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}