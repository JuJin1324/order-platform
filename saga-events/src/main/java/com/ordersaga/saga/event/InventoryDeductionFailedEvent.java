package com.ordersaga.saga.event;

public record InventoryDeductionFailedEvent(
        String orderId,
        String sku,
        String reason
) {
}
