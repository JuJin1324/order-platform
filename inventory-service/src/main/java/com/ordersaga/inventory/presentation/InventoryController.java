package com.ordersaga.inventory.presentation;

import java.util.Map;

import com.ordersaga.inventory.application.InventoryApplicationService;
import com.ordersaga.inventory.application.InventoryResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {
    private final InventoryApplicationService inventoryApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @GetMapping("/api/inventory/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "inventory-service",
                "status", "ok"
        );
    }

    @GetMapping("/api/inventory/{sku}")
    public InventoryResult getInventory(@PathVariable String sku) {
        return inventoryApplicationService.getInventory(sku);
    }
}
