package com.ordersaga.order.application;

import java.math.BigDecimal;

import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 응답")
public record OrderResult(
        @Schema(description = "주문 고유 ID (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String orderId,

        @Schema(description = "주문 상태")
        OrderStatus status,

        @Schema(description = "주문한 상품 ID", example = "ITEM-001")
        String sku,

        @Schema(description = "주문 수량", example = "2")
        Integer quantity,

        @Schema(description = "결제 금액", example = "29900")
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
