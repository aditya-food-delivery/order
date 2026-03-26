package com.aditya.order_service.event.util;

import com.aditya.contracts.event.EventTypes;
import org.springframework.stereotype.Component;

@Component
public class EventRoutingKeyResolver {

    public String resolve(String eventType) {

        return switch (eventType) {
            case EventTypes.ORDER_CREATED -> "order.created";
            case EventTypes.ORDER_CONFIRMED -> "order.confirmed";
            case EventTypes.ORDER_FAILED -> "order.failed";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}