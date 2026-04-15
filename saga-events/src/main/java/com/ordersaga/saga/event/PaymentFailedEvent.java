package com.ordersaga.saga.event;

public record PaymentFailedEvent(
        String orderId,
        String reason
) {
}
