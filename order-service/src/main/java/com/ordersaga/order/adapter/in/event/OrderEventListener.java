package com.ordersaga.order.adapter.in.event;

import com.ordersaga.order.application.OrderEventProcessor;
import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.PaymentCancelledEvent;
import com.ordersaga.saga.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderEventProcessor orderEventProcessor;

    public OrderEventListener(OrderEventProcessor orderEventProcessor) {
        this.orderEventProcessor = orderEventProcessor;
    }

    @KafkaListener(topics = SagaTopics.INVENTORY_DEDUCTED, groupId = "${spring.application.name}")
    public void onInventoryDeducted(InventoryDeductedEvent event) {
        log.info("Received inventory-deducted event for orderId={} sku={}", event.orderId(), event.sku());
        orderEventProcessor.handleInventoryDeducted(event);
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_FAILED, groupId = "${spring.application.name}")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment-failed event for orderId={}", event.orderId());
        orderEventProcessor.handlePaymentFailed(event);
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_CANCELLED, groupId = "${spring.application.name}")
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        log.info("Received payment-cancelled event for orderId={} paymentId={}", event.orderId(), event.paymentId());
        orderEventProcessor.handlePaymentCancelled(event);
    }
}
