package com.ordersaga.order.application;

import com.ordersaga.saga.event.InventoryDeductedEvent;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProcessor {
    private final OrderApplicationService orderApplicationService;

    public OrderEventProcessor(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    public OrderResult handleInventoryDeducted(InventoryDeductedEvent event) {
        return orderApplicationService.confirmOrder(event.orderId());
    }
}
