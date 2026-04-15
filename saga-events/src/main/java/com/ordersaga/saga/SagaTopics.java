package com.ordersaga.saga;

public final class SagaTopics {
    // 순방향
    public static final String ORDER_CREATED = "order-created";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String INVENTORY_DEDUCTED = "inventory-deducted";

    // 보상
    public static final String INVENTORY_DEDUCTION_FAILED = "inventory-deduction-failed";
    public static final String PAYMENT_FAILED = "payment-failed";
    public static final String PAYMENT_CANCELLED = "payment-cancelled";

    private SagaTopics() {
    }
}
