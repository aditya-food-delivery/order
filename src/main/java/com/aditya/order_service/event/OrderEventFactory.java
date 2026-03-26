package com.aditya.order_service.event;



import com.aditya.contracts.event.*;
import com.aditya.contracts.order.OrderCreatedEvent;

import java.util.UUID;
import com.aditya.order_service.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventFactory {

    public DomainEvent<OrderCreatedEvent> createOrderCreatedEvent(UUID aggregrateId , OrderCreatedEvent payload) {


        return DomainEvent.of(
                EventTypes.ORDER_CREATED,
                EventVersions.V1,
                AggregateTypes.ORDER,
                aggregrateId,
                payload
        );
    }
}