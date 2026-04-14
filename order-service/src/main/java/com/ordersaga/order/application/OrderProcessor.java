package com.ordersaga.order.application;

import com.ordersaga.order.application.port.out.PublishOrderCreatedPort;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessor {
    private final OrderApplicationService orderApplicationService;
    private final PublishOrderCreatedPort publishOrderCreatedPort;

    public OrderProcessor(
            OrderApplicationService orderApplicationService,
            PublishOrderCreatedPort publishOrderCreatedPort
    ) {
        this.orderApplicationService = orderApplicationService;
        this.publishOrderCreatedPort = publishOrderCreatedPort;
    }

    public OrderResult processOrder(CreateOrderCommand command) {
        OrderResult created = orderApplicationService.createOrder(command);
        publishOrderCreatedPort.publishOrderCreated(created);
        return created;
    }
}
