package com.ordersaga.inventory.fixture;

import com.ordersaga.inventory.application.DeductInventoryCommand;

import static com.ordersaga.inventory.fixture.InventoryFixtureValues.DEDUCT_QUANTITY;
import static com.ordersaga.inventory.fixture.InventoryFixtureValues.SKU;

public class DeductInventoryCommandFixture {

    public static DeductInventoryCommand normal() {
        return new DeductInventoryCommand(SKU, DEDUCT_QUANTITY);
    }
}
