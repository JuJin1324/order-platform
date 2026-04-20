package com.ordersaga.order.adapter.in.web.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @Schema(description = "주문할 상품 ID", example = "ITEM-001")
        @NotBlank(message = "sku is required")
        String sku,

        @Schema(description = "주문 수량", example = "2")
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity,

        @Schema(description = "결제 금액 (원 단위, 최소 1)", example = "29900")
        @NotNull(message = "amount is required")
        @DecimalMin(value = "1", message = "amount must be at least 1")
        BigDecimal amount
) {
}
