package com.ordersaga.order.application;

import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.PaymentCancelledEvent;
import com.ordersaga.saga.event.PaymentFailedEvent;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProcessor {
    private final OrderApplicationService orderApplicationService;

    public OrderEventProcessor(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    public void handleInventoryDeducted(InventoryDeductedEvent event) {
        orderApplicationService.confirmOrder(event.orderId());
    }

    public void handlePaymentFailed(PaymentFailedEvent event) {
        orderApplicationService.cancelOrder(event.orderId());
    }

    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        orderApplicationService.cancelOrder(event.orderId());
    }
}
