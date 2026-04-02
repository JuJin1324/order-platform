package com.ordersaga.payment.fixture;

import com.ordersaga.payment.presentation.dto.ChargePaymentRequest;

import static com.ordersaga.payment.fixture.PaymentFixtureValues.*;

public class ChargePaymentRequestFixture {

    public static ChargePaymentRequest normal() {
        return new ChargePaymentRequest(ORDER_ID, AMOUNT, SKU, QUANTITY, false);
    }

    public static ChargePaymentRequest withForceInventoryFailure() {
        return new ChargePaymentRequest(ORDER_ID, AMOUNT, SKU, QUANTITY, true);
    }

    public static ChargePaymentRequest missingOrderId() {
        return new ChargePaymentRequest(null, AMOUNT, SKU, QUANTITY, false);
    }
}
