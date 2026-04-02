package com.ordersaga.order.application;

import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderRepository;
import com.ordersaga.order.infrastructure.PaymentClient;
import org.springframework.stereotype.Service;

@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public OrderApplicationService(OrderRepository orderRepository, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    public OrderResult createOrder(CreateOrderCommand command) {
        Order order = Order.create(command.sku(), command.quantity(), command.amount());
        orderRepository.save(order);

        boolean paymentSuccess = paymentClient.chargePayment(
                order.getOrderId(), command.amount(), command.sku(), command.quantity(), command.forceInventoryFailure()
        );
        if (paymentSuccess) {
            order.confirm();
        } else {
            order.fail();
        }

        orderRepository.save(order);

        return OrderResult.from(order);
    }

    public OrderResult getOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));

        return OrderResult.from(order);
    }
}
