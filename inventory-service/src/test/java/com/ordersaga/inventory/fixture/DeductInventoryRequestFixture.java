package com.ordersaga.inventory.fixture;

import com.ordersaga.inventory.presentation.dto.DeductInventoryRequest;

import static com.ordersaga.inventory.fixture.InventoryFixtureValues.DEDUCT_QUANTITY;
import static com.ordersaga.inventory.fixture.InventoryFixtureValues.SKU;

public class DeductInventoryRequestFixture {

    public static DeductInventoryRequest normal() {
        return new DeductInventoryRequest(SKU, DEDUCT_QUANTITY, false);
    }

    public static DeductInventoryRequest withForceFailure() {
        return new DeductInventoryRequest(SKU, DEDUCT_QUANTITY, true);
    }

    public static DeductInventoryRequest missingSku() {
        return new DeductInventoryRequest(null, DEDUCT_QUANTITY, false);
    }
}
