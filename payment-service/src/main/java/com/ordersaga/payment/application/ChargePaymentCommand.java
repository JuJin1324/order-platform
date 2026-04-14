package com.ordersaga.payment.application;

import java.math.BigDecimal;

public record ChargePaymentCommand(
        String orderId,
        BigDecimal amount,
        String sku,
        Integer quantity
) {
}
