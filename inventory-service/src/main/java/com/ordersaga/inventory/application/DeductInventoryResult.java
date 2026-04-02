package com.ordersaga.inventory.application;

import com.ordersaga.inventory.domain.Inventory;

public record DeductInventoryResult(
        String sku,
        Integer deductedQuantity,
        Integer remainingQuantity
) {
    public static DeductInventoryResult from(Inventory inventory, Integer deductedQuantity) {
        return new DeductInventoryResult(
                inventory.getSku(),
                deductedQuantity,
                inventory.getAvailableQuantity()
        );
    }
}
