package com.ordersaga.order.fixture;

import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.domain.OrderStatus;

public class OrderResultFixture {

    private OrderResultFixture() {
    }

    public static OrderResult created() {
        return new OrderResult(
                OrderFixtureValues.ORDER_ID,
                OrderStatus.CREATED,
                OrderFixtureValues.SKU,
                OrderFixtureValues.QUANTITY,
                OrderFixtureValues.AMOUNT
        );
    }

    public static OrderResult confirmed() {
        return new OrderResult(
                OrderFixtureValues.ORDER_ID,
                OrderStatus.CONFIRMED,
                OrderFixtureValues.SKU,
                OrderFixtureValues.QUANTITY,
                OrderFixtureValues.AMOUNT
        );
    }

    public static OrderResult failed() {
        return new OrderResult(
                OrderFixtureValues.ORDER_ID,
                OrderStatus.FAILED,
                OrderFixtureValues.SKU,
                OrderFixtureValues.QUANTITY,
                OrderFixtureValues.AMOUNT
        );
    }
}
