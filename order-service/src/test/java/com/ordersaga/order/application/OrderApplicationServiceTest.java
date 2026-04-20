package com.ordersaga.order.application;

import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderNotFoundException;
import com.ordersaga.order.domain.OrderRepository;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.fixture.OrderFixtureValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderApplicationService orderApplicationService;

    @BeforeEach
    void setUp() {
        orderApplicationService = new OrderApplicationService(orderRepository);
    }

    @Test
    @DisplayName("주문 생성 시 CREATED 상태로 저장하고 결과를 반환한다")
    void createOrder_savesOrderAndReturnsCreated() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand(
                OrderFixtureValues.SKU,
                OrderFixtureValues.QUANTITY,
                OrderFixtureValues.AMOUNT
        );

        // When
        OrderResult result = orderApplicationService.createOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.sku()).isEqualTo(OrderFixtureValues.SKU);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    @DisplayName("주문 확정 시 CONFIRMED 상태로 전이하고 저장한다")
    void confirmOrder_transitionsToConfirmedAndSaves() {
        // Given
        Order order = Order.create(OrderFixtureValues.SKU, OrderFixtureValues.QUANTITY, OrderFixtureValues.AMOUNT);
        given(orderRepository.findByOrderId(OrderFixtureValues.ORDER_ID)).willReturn(Optional.of(order));

        // When
        OrderResult result = orderApplicationService.confirmOrder(OrderFixtureValues.ORDER_ID);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        then(orderRepository).should().save(order);
    }

    @Test
    @DisplayName("주문 취소 시 CANCELLED 상태로 전이하고 저장한다")
    void cancelOrder_transitionsToCancelledAndSaves() {
        // Given
        Order order = Order.create(OrderFixtureValues.SKU, OrderFixtureValues.QUANTITY, OrderFixtureValues.AMOUNT);
        given(orderRepository.findByOrderId(OrderFixtureValues.ORDER_ID)).willReturn(Optional.of(order));

        // When
        OrderResult result = orderApplicationService.cancelOrder(OrderFixtureValues.ORDER_ID);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        then(orderRepository).should().save(order);
    }

    @Test
    @DisplayName("존재하지 않는 orderId로 조회하면 OrderNotFoundException이 발생한다")
    void findOrder_whenNotFound_throwsOrderNotFoundException() {
        // Given
        given(orderRepository.findByOrderId(OrderFixtureValues.ORDER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.confirmOrder(OrderFixtureValues.ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(OrderFixtureValues.ORDER_ID);
    }
}
