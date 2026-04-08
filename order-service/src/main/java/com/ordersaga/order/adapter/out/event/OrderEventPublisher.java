package com.ordersaga.order.adapter.out.event;

import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.application.port.out.PublishOrderCreatedPort;
import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.saga.mode", havingValue = "kafka")
public class OrderEventPublisher implements PublishOrderCreatedPort {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaOperations<Object, Object> kafkaOperations;

    public OrderEventPublisher(KafkaOperations<Object, Object> kafkaOperations) {
        this.kafkaOperations = kafkaOperations;
    }

    @Override
    public void publishOrderCreated(OrderResult orderResult) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderResult.orderId(),
                orderResult.sku(),
                orderResult.quantity(),
                orderResult.amount()
        );
        log.info("Publishing order-created event for orderId={}", event.orderId());
        kafkaOperations.send(SagaTopics.ORDER_CREATED, event.orderId(), event);
    }
}
