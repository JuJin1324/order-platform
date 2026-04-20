package com.ordersaga.order.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "오류 응답")
public record ErrorResponse(
        @Schema(description = "오류 코드", example = "ORDER_NOT_FOUND")
        String code,

        @Schema(description = "오류 메시지", example = "주문을 찾을 수 없습니다: abc-123")
        String message,

        @Schema(description = "오류 발생 시각 (ISO 8601)")
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}
