package com.ordersaga.payment.infrastructure;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {
    private final RestClient restClient;

    public InventoryClient(@Value("${inventory-service.url}") String inventoryServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(inventoryServiceUrl)
                .build();
    }

    public boolean deductInventory(String sku, Integer quantity, boolean forceFailure) {
        Map<String, Object> request = Map.of(
                "sku", sku,
                "quantity", quantity,
                "forceFailure", forceFailure
        );

        try {
            restClient.post()
                    .uri("/internal/inventory/deduct")
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
