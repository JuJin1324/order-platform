package com.ordersaga.order.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.domain.OrderStatusHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "주문 상태 이력 항목")
public record OrderStatusHistoryResult(
        @Schema(description = "상태")
        OrderStatus status,

        @Schema(description = "상태 전이 시각")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime changedAt
) {
    public static OrderStatusHistoryResult from(OrderStatusHistory history) {
        return new OrderStatusHistoryResult(history.getStatus(), history.getChangedAt());
    }
}
