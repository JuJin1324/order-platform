package com.ordersaga.order.fixture;

import com.ordersaga.order.application.CreateOrderCommand;

import static com.ordersaga.order.fixture.OrderFixtureValues.*;

public class CreateOrderCommandFixture {

    public static CreateOrderCommand normal() {
        return new CreateOrderCommand(SKU, QUANTITY, AMOUNT);
    }
}
