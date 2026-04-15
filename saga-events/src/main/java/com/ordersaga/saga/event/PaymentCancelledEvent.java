package com.ordersaga.saga.event;

public record PaymentCancelledEvent(
        String orderId,
        String paymentId
) {
}
