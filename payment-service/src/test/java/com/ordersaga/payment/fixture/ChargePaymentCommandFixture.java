package com.ordersaga.payment.fixture;

import com.ordersaga.payment.application.ChargePaymentCommand;

import static com.ordersaga.payment.fixture.PaymentFixtureValues.*;

public class ChargePaymentCommandFixture {

    public static ChargePaymentCommand normal() {
        return new ChargePaymentCommand(ORDER_ID, AMOUNT, SKU, QUANTITY, false);
    }

    public static ChargePaymentCommand withForceInventoryFailure() {
        return new ChargePaymentCommand(ORDER_ID, AMOUNT, SKU, QUANTITY, true);
    }
}
