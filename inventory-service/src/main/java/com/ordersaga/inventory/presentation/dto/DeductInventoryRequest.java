package com.ordersaga.inventory.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeductInventoryRequest(
        @NotBlank(message = "sku is required")
        String sku,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity,

        boolean forceFailure
) {
}
