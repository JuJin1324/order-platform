package com.ordersaga.order.application;

import com.ordersaga.order.infrastructure.PaymentClient;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessor {
    private final OrderApplicationService orderApplicationService;
    private final PaymentClient paymentClient;

    public OrderProcessor(OrderApplicationService orderApplicationService, PaymentClient paymentClient) {
        this.orderApplicationService = orderApplicationService;
        this.paymentClient = paymentClient;
    }

    public OrderResult processOrder(CreateOrderCommand command) {
        OrderResult created = orderApplicationService.createOrder(command);

        boolean paymentSuccess = paymentClient.chargePayment(
                created.orderId(), command.amount(), command.sku(), command.quantity(), command.forceInventoryFailure()
        );

        if (paymentSuccess) {
            return orderApplicationService.confirmOrder(created.orderId());
        } else {
            return orderApplicationService.failOrder(created.orderId());
        }
    }
}
