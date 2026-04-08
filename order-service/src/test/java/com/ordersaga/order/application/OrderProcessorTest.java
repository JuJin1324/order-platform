package com.ordersaga.order.application;

import com.ordersaga.order.application.port.out.ChargePaymentPort;
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

import java.util.Optional;

import static com.ordersaga.order.fixture.OrderFixtureValues.ORDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderApplicationService orderApplicationService;

    @Mock
    private ChargePaymentPort chargePaymentPort;

    @Mock
    private PublishOrderCreatedPort publishOrderCreatedPort;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() {
        orderProcessor = new OrderProcessor(orderApplicationService, chargePaymentPort, Optional.empty(), "rest");
    }

    @Test
    @DisplayName("결제 성공 시 주문 상태는 CONFIRMED")
    void paymentSuccess_orderStatusConfirmed() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.normal();
        OrderResult createdOrder = OrderResultFixture.created();
        OrderResult confirmedOrder = OrderResultFixture.confirmed();

        given(orderApplicationService.createOrder(command)).willReturn(createdOrder);
        given(chargePaymentPort.chargePayment(
                eq(ORDER_ID),
                eq(command.amount()),
                eq(command.sku()),
                eq(command.quantity()),
                eq(false)
        ))
                .willReturn(true);
        given(orderApplicationService.confirmOrder(ORDER_ID)).willReturn(confirmedOrder);

        // When
        OrderResult result = orderProcessor.processOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        then(publishOrderCreatedPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("결제 실패 시 주문 상태는 FAILED")
    void paymentFailure_orderStatusFailed() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.withForceInventoryFailure();
        OrderResult createdOrder = OrderResultFixture.created();
        OrderResult failedOrder = OrderResultFixture.failed();

        given(orderApplicationService.createOrder(command)).willReturn(createdOrder);
        given(chargePaymentPort.chargePayment(
                eq(ORDER_ID),
                eq(command.amount()),
                eq(command.sku()),
                eq(command.quantity()),
                eq(true)
        ))
                .willReturn(false);
        given(orderApplicationService.failOrder(ORDER_ID)).willReturn(failedOrder);

        // When
        OrderResult result = orderProcessor.processOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.FAILED);
        then(publishOrderCreatedPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Kafka 모드에서는 order-created 이벤트를 발행하고 CREATED 상태로 즉시 응답한다")
    void kafkaMode_publishesOrderCreatedAndReturnsCreated() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.normal();
        OrderResult createdOrder = OrderResultFixture.created();
        orderProcessor = new OrderProcessor(
                orderApplicationService,
                chargePaymentPort,
                Optional.of(publishOrderCreatedPort),
                "kafka"
        );

        given(orderApplicationService.createOrder(command)).willReturn(createdOrder);

        // When
        OrderResult result = orderProcessor.processOrder(command);

        // Then
        assertThat(result).isEqualTo(createdOrder);
        then(orderApplicationService).should().createOrder(command);
        then(publishOrderCreatedPort).should().publishOrderCreated(createdOrder);
        then(chargePaymentPort).shouldHaveNoInteractions();
        then(orderApplicationService).shouldHaveNoMoreInteractions();
    }
}
