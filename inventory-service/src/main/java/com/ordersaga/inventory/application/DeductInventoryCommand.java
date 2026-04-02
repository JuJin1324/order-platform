package com.ordersaga.inventory.application;

public record DeductInventoryCommand(
        String sku,
        Integer quantity,
        boolean forceFailure
) {
}
