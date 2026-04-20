package com.ordersaga.order.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상태")
public enum OrderStatus {
    @Schema(description = "주문 생성됨 — 결제·재고 Saga 진행 중")
    CREATED,

    @Schema(description = "결제·재고 확인 완료")
    CONFIRMED,

    @Schema(description = "결제 실패 또는 재고 부족으로 취소됨")
    CANCELLED
}
