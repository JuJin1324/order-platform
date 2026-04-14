package com.ordersaga.order.application;

import com.ordersaga.order.application.port.out.PublishOrderCreatedPort;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.fixture.CreateOrderCommandFixture;
import com.ordersaga.order.fixture.OrderResultFixture;
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
class OrderProcessorTest {

    @Mock
    private OrderApplicationService orderApplicationService;

    @Mock
    private PublishOrderCreatedPort publishOrderCreatedPort;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() {
        orderProcessor = new OrderProcessor(orderApplicationService, publishOrderCreatedPort);
    }

    @Test
    @DisplayName("주문 생성 시 order-created 이벤트를 발행하고 CREATED 상태로 즉시 응답한다")
    void createOrder_publishesEventAndReturnsCreated() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.normal();
        OrderResult createdOrder = OrderResultFixture.created();
        given(orderApplicationService.createOrder(command)).willReturn(createdOrder);

        // When
        OrderResult result = orderProcessor.processOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        then(orderApplicationService).should().createOrder(command);
        then(publishOrderCreatedPort).should().publishOrderCreated(createdOrder);
        then(orderApplicationService).shouldHaveNoMoreInteractions();
    }
}
