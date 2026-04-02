package com.ordersaga.order.application;

import java.math.BigDecimal;

import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderStatus;

public record OrderResult(
        String orderId,
        OrderStatus status,
        String sku,
        Integer quantity,
        BigDecimal amount
) {
    public static OrderResult from(Order order) {
        return new OrderResult(
                order.getOrderId(),
                order.getStatus(),
                order.getSku(),
                order.getQuantity(),
                order.getAmount()
        );
    }
}
