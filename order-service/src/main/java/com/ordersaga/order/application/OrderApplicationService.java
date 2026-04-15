package com.ordersaga.order.application;

import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;

    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResult createOrder(CreateOrderCommand command) {
        Order order = Order.create(command.sku(), command.quantity(), command.amount());
        orderRepository.save(order);
        return OrderResult.from(order);
    }

    public OrderResult confirmOrder(String orderId) {
        Order order = findOrder(orderId);
        order.confirm();
        orderRepository.save(order);
        return OrderResult.from(order);
    }

    public OrderResult cancelOrder(String orderId) {
        Order order = findOrder(orderId);
        order.cancel();
        orderRepository.save(order);
        return OrderResult.from(order);
    }

    public OrderResult failOrder(String orderId) {
        Order order = findOrder(orderId);
        order.fail();
        orderRepository.save(order);
        return OrderResult.from(order);
    }

    public OrderResult getOrder(String orderId) {
        Order order = findOrder(orderId);
        return OrderResult.from(order);
    }

    private Order findOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));
    }
}
