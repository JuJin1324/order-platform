package com.ordersaga.inventory.application;

import com.ordersaga.inventory.infrastructure.kafka.InventoryEventPublisher;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.InventoryDeductionFailedEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ordersaga.inventory.fixture.InventoryFixtureValues.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InventoryEventProcessorTest {

    @Mock
    private InventoryApplicationService inventoryApplicationService;

    @Mock
    private InventoryEventPublisher inventoryEventPublisher;

    private InventoryEventProcessor inventoryEventProcessor;

    @BeforeEach
    void setUp() {
        inventoryEventProcessor = new InventoryEventProcessor(inventoryApplicationService, inventoryEventPublisher);
    }

    @Test
    @DisplayName("재고 차감 성공 시 inventory-deducted 이벤트를 발행한다")
    void handlePaymentCompleted_deductsInventoryAndPublishesNextEvent() {
        // Given
        PaymentCompletedEvent receivedEvent = receivedPaymentCompletedEvent();
        DeductInventoryResult deductedInventory = expectedDeductedInventory();

        given(inventoryApplicationService.deductInventory(any(DeductInventoryCommand.class)))
                .willReturn(deductedInventory);

        // When
        inventoryEventProcessor.handlePaymentCompleted(receivedEvent);

        // Then
        then(inventoryApplicationService).should().deductInventory(expectedDeductInventoryCommand(receivedEvent));
        then(inventoryEventPublisher).should().publishInventoryDeducted(expectedInventoryDeductedEvent(receivedEvent));
        then(inventoryEventPublisher).should(never()).publishInventoryDeductionFailed(any());
    }

    @Test
    @DisplayName("재고 부족 시 inventory-deduction-failed 이벤트를 발행한다")
    void handlePaymentCompleted_whenInsufficientInventory_publishesDeductionFailedEvent() {
        // Given
        PaymentCompletedEvent receivedEvent = receivedPaymentCompletedEvent();
        String failureReason = "not enough inventory for sku: " + SKU;

        given(inventoryApplicationService.deductInventory(any(DeductInventoryCommand.class)))
                .willThrow(new IllegalStateException(failureReason));

        // When
        inventoryEventProcessor.handlePaymentCompleted(receivedEvent);

        // Then
        then(inventoryEventPublisher).should().publishInventoryDeductionFailed(
                new InventoryDeductionFailedEvent(ORDER_ID, SKU, failureReason)
        );
        then(inventoryEventPublisher).should(never()).publishInventoryDeducted(any());
    }

    private PaymentCompletedEvent receivedPaymentCompletedEvent() {
        return new PaymentCompletedEvent(
                ORDER_ID,
                PAYMENT_ID,
                AMOUNT,
                SKU,
                DEDUCT_QUANTITY
        );
    }

    private DeductInventoryCommand expectedDeductInventoryCommand(PaymentCompletedEvent receivedPaymentCompletedEvent) {
        return new DeductInventoryCommand(
                receivedPaymentCompletedEvent.sku(),
                receivedPaymentCompletedEvent.quantity()
        );
    }

    private DeductInventoryResult expectedDeductedInventory() {
        return new DeductInventoryResult(SKU, DEDUCT_QUANTITY, REMAINING_QUANTITY);
    }

    private InventoryDeductedEvent expectedInventoryDeductedEvent(PaymentCompletedEvent receivedPaymentCompletedEvent) {
        return new InventoryDeductedEvent(
                receivedPaymentCompletedEvent.orderId(),
                receivedPaymentCompletedEvent.sku(),
                receivedPaymentCompletedEvent.quantity(),
                REMAINING_QUANTITY
        );
    }
}
