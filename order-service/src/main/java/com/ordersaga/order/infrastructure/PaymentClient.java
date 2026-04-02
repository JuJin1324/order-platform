package com.ordersaga.order.infrastructure;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {
    private final RestClient restClient;

    public PaymentClient(@Value("${payment-service.url}") String paymentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }

    public boolean chargePayment(String orderId, BigDecimal amount, String sku, Integer quantity, boolean forceInventoryFailure) {
        Map<String, Object> request = Map.of(
                "orderId", orderId,
                "amount", amount,
                "sku", sku,
                "quantity", quantity,
                "forceInventoryFailure", forceInventoryFailure
        );

        try {
            restClient.post()
                    .uri("/internal/payments")
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
