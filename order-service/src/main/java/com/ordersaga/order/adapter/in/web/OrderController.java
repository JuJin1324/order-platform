package com.ordersaga.order.adapter.in.web;

import java.util.Map;

import com.ordersaga.order.application.CreateOrderCommand;
import com.ordersaga.order.application.OrderApplicationService;
import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.application.OrderProcessor;
import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderProcessor orderProcessor;
    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderProcessor orderProcessor, OrderApplicationService orderApplicationService) {
        this.orderProcessor = orderProcessor;
        this.orderApplicationService = orderApplicationService;
    }

    @Operation(summary = "헬스 체크", description = "서비스 정상 가동 여부를 확인한다.")
    @ApiResponse(responseCode = "200", description = "서비스 정상")
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "order-service",
                "status", "ok"
        );
    }

    @Operation(summary = "주문 조회", description = "주문 ID로 단건 주문을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 조회 성공"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{orderId}")
    public OrderResult getOrder(@PathVariable String orderId) {
        return orderApplicationService.getOrder(orderId);
    }

    @Operation(
            summary = "주문 생성",
            description = "새 주문을 생성하고 결제·재고 Saga를 비동기로 시작한다. " +
                          "응답은 Saga 완료 전 CREATED 상태로 즉시 반환된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 생성 성공 (Saga 진행 중)"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (sku 누락, quantity ≤ 0, amount < 0.01)")
    })
    @PostMapping
    public OrderResult createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.sku(),
                request.quantity(),
                request.amount()
        );
        return orderProcessor.processOrder(command);
    }
}
