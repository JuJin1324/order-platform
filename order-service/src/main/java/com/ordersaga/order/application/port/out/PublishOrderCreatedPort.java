package com.ordersaga.order.application.port.out;

import com.ordersaga.order.application.OrderResult;

public interface PublishOrderCreatedPort {

    void publishOrderCreated(OrderResult orderResult);
}
