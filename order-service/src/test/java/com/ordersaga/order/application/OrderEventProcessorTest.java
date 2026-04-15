package com.ordersaga.order.application;

import com.ordersaga.order.fixture.OrderFixtureValues;
import com.ordersaga.order.fixture.OrderResultFixture;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.PaymentCancelledEvent;
import com.ordersaga.saga.event.PaymentFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderEventProcessorTest {
    private static final int REMAINING_QUANTITY = 8;

    @Mock
    private OrderApplicationService orderApplicationService;

    private OrderEventProcessor orderEventProcessor;

    @BeforeEach
    void setUp() {
        orderEventProcessor = new OrderEventProcessor(orderApplicationService);
    }

    @Test
    @DisplayName("inventory-deducted 이벤트를 수신하면 주문을 확정한다")
    void handleInventoryDeducted_confirmsOrder() {
        // Given
        InventoryDeductedEvent receivedEvent = receivedInventoryDeductedEvent();
        given(orderApplicationService.confirmOrder(receivedEvent.orderId()))
                .willReturn(OrderResultFixture.confirmed());

        // When
        orderEventProcessor.handleInventoryDeducted(receivedEvent);

        // Then
        then(orderApplicationService).should().confirmOrder(receivedEvent.orderId());
    }

    @Test
    @DisplayName("payment-failed 이벤트를 수신하면 주문을 취소한다")
    void handlePaymentFailed_cancelsOrder() {
        // Given
        PaymentFailedEvent receivedEvent = receivedPaymentFailedEvent();
        given(orderApplicationService.cancelOrder(receivedEvent.orderId()))
                .willReturn(OrderResultFixture.cancelled());

        // When
        orderEventProcessor.handlePaymentFailed(receivedEvent);

        // Then
        then(orderApplicationService).should().cancelOrder(receivedEvent.orderId());
    }

    @Test
    @DisplayName("payment-cancelled 이벤트를 수신하면 주문을 취소한다")
    void handlePaymentCancelled_cancelsOrder() {
        // Given
        PaymentCancelledEvent receivedEvent = receivedPaymentCancelledEvent();
        given(orderApplicationService.cancelOrder(receivedEvent.orderId()))
                .willReturn(OrderResultFixture.cancelled());

        // When
        orderEventProcessor.handlePaymentCancelled(receivedEvent);

        // Then
        then(orderApplicationService).should().cancelOrder(receivedEvent.orderId());
    }

    private InventoryDeductedEvent receivedInventoryDeductedEvent() {
        return new InventoryDeductedEvent(
                OrderFixtureValues.ORDER_ID,
                OrderFixtureValues.SKU,
                OrderFixtureValues.QUANTITY,
                REMAINING_QUANTITY
        );
    }

    private PaymentFailedEvent receivedPaymentFailedEvent() {
        return new PaymentFailedEvent(OrderFixtureValues.ORDER_ID, "payment processing failed");
    }

    private PaymentCancelledEvent receivedPaymentCancelledEvent() {
        return new PaymentCancelledEvent(OrderFixtureValues.ORDER_ID, "test-payment-001");
    }
}
