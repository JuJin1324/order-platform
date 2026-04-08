package com.ordersaga.order.application;

import com.ordersaga.order.fixture.OrderFixtureValues;
import com.ordersaga.order.fixture.OrderResultFixture;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
        OrderResult confirmedOrder = OrderResultFixture.confirmed();

        given(orderApplicationService.confirmOrder(receivedEvent.orderId()))
                .willReturn(confirmedOrder);

        // When
        OrderResult result = orderEventProcessor.handleInventoryDeducted(receivedEvent);

        // Then
        assertThat(result).isEqualTo(confirmedOrder);
        then(orderApplicationService).should().confirmOrder(receivedEvent.orderId());
    }

    private InventoryDeductedEvent receivedInventoryDeductedEvent() {
        return new InventoryDeductedEvent(
                OrderFixtureValues.ORDER_ID,
                OrderFixtureValues.SKU,
                OrderFixtureValues.QUANTITY,
                REMAINING_QUANTITY
        );
    }
}
