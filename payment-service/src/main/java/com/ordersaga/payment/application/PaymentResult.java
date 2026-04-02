package com.ordersaga.payment.application;

import java.math.BigDecimal;

import com.ordersaga.payment.domain.Payment;
import com.ordersaga.payment.domain.PaymentStatus;

public record PaymentResult(
        String paymentId,
        String orderId,
        PaymentStatus status,
        BigDecimal amount
) {
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getAmount()
        );
    }
}
