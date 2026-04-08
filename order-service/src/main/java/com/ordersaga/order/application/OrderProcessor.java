package com.ordersaga.order.application;

import com.ordersaga.order.application.port.out.ChargePaymentPort;
import com.ordersaga.order.application.port.out.PublishOrderCreatedPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderProcessor {
    private final OrderApplicationService orderApplicationService;
    private final ChargePaymentPort chargePaymentPort;
    private final Optional<PublishOrderCreatedPort> publishOrderCreatedPort;
    private final String sagaMode;

    public OrderProcessor(
            OrderApplicationService orderApplicationService,
            ChargePaymentPort chargePaymentPort,
            Optional<PublishOrderCreatedPort> publishOrderCreatedPort,
            @Value("${app.saga.mode:rest}") String sagaMode
    ) {
        this.orderApplicationService = orderApplicationService;
        this.chargePaymentPort = chargePaymentPort;
        this.publishOrderCreatedPort = publishOrderCreatedPort;
        this.sagaMode = sagaMode;
    }

    public OrderResult processOrder(CreateOrderCommand command) {
        OrderResult created = orderApplicationService.createOrder(command);

        if (isKafkaMode()) {
            publishOrderCreatedPort.orElseThrow(() ->
                    new IllegalStateException("PublishOrderCreatedPort bean is required when app.saga.mode=kafka")
            ).publishOrderCreated(created);
            return created;
        }

        boolean paymentSuccess = chargePaymentPort.chargePayment(
                created.orderId(), command.amount(), command.sku(), command.quantity(), command.forceInventoryFailure()
        );

        if (paymentSuccess) {
            return orderApplicationService.confirmOrder(created.orderId());
        } else {
            return orderApplicationService.failOrder(created.orderId());
        }
    }

    private boolean isKafkaMode() {
        return "kafka".equalsIgnoreCase(sagaMode);
    }
}
