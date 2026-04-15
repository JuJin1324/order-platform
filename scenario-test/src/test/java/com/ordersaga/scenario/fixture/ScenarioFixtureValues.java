package com.ordersaga.scenario.fixture;

import java.math.BigDecimal;

public class ScenarioFixtureValues {
    public static final String SKU = "sku-001";
    public static final Integer QUANTITY = 2;
    public static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    public static final BigDecimal OVER_LIMIT_AMOUNT = BigDecimal.valueOf(1_000_001);
    public static final Integer INITIAL_INVENTORY_QUANTITY = 10;
    public static final Integer REMAINING_INVENTORY_QUANTITY = 8;
}
