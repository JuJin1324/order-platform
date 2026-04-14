package com.ordersaga.order.application;

import java.math.BigDecimal;

public record CreateOrderCommand(
        String sku,
        Integer quantity,
        BigDecimal amount
) {
}
