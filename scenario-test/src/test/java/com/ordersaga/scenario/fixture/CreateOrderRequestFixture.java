package com.ordersaga.scenario.fixture;

import com.ordersaga.order.presentation.dto.CreateOrderRequest;

public class CreateOrderRequestFixture {

    public static CreateOrderRequest normal() {
        return new CreateOrderRequest(
                ScenarioFixtureValues.SKU,
                ScenarioFixtureValues.QUANTITY,
                ScenarioFixtureValues.AMOUNT,
                false
        );
    }

    public static CreateOrderRequest withInventoryFailure() {
        return new CreateOrderRequest(
                ScenarioFixtureValues.SKU,
                ScenarioFixtureValues.QUANTITY,
                ScenarioFixtureValues.AMOUNT,
                true
        );
    }
}
