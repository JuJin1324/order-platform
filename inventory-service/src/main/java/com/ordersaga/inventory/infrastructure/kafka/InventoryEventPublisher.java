package com.ordersaga.inventory.infrastructure.kafka;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.InventoryDeductionFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaOperations<Object, Object> kafkaOperations;

    public InventoryEventPublisher(KafkaOperations<Object, Object> kafkaOperations) {
        this.kafkaOperations = kafkaOperations;
    }

    public void publishInventoryDeducted(InventoryDeductedEvent event) {
        log.info("Publishing inventory-deducted event for orderId={} sku={}", event.orderId(), event.sku());
        kafkaOperations.send(SagaTopics.INVENTORY_DEDUCTED, event.orderId(), event);
    }

    public void publishInventoryDeductionFailed(InventoryDeductionFailedEvent event) {
        log.info("Publishing inventory-deduction-failed event for orderId={} sku={} reason={}", event.orderId(), event.sku(), event.reason());
        kafkaOperations.send(SagaTopics.INVENTORY_DEDUCTION_FAILED, event.orderId(), event);
    }
}
