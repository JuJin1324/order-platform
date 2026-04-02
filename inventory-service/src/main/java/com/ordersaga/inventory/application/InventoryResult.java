package com.ordersaga.inventory.application;

import com.ordersaga.inventory.domain.Inventory;

public record InventoryResult(
        String sku,
        Integer availableQuantity
) {
    public static InventoryResult from(Inventory inventory) {
        return new InventoryResult(inventory.getSku(), inventory.getAvailableQuantity());
    }
}
