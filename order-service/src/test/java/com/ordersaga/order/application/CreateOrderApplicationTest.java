package com.ordersaga.order.application;

import com.ordersaga.order.fixture.CreateOrderCommandFixture;
import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderRepository;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.infrastructure.PaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CreateOrderApplicationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentClient paymentClient;

    private OrderApplicationService sut;

    @BeforeEach
    void setUp() {
        sut = new OrderApplicationService(orderRepository, paymentClient);
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("결제 성공 시 주문 상태는 CONFIRMED")
    void paymentSuccess_orderStatusConfirmed() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.normal();
        given(paymentClient.chargePayment(any(), eq(command.amount()), eq(command.sku()), eq(command.quantity()), eq(command.forceInventoryFailure())))
                .willReturn(true);

        // When
        OrderResult result = sut.createOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("결제 실패 시 주문 상태는 FAILED")
    void paymentFailure_orderStatusFailed() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.withForceInventoryFailure();
        given(paymentClient.chargePayment(any(), eq(command.amount()), eq(command.sku()), eq(command.quantity()), eq(command.forceInventoryFailure())))
                .willReturn(false);

        // When
        OrderResult result = sut.createOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.FAILED);
    }
}
