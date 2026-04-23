package com.ordersaga.order.application;

import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.domain.OrderStatusHistory;
import com.ordersaga.order.domain.OrderStatusHistoryRepository;
import com.ordersaga.order.fixture.OrderFixtureValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderStatusHistoryAspectTest {

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    private OrderStatusHistoryAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new OrderStatusHistoryAspect(orderStatusHistoryRepository);
    }

    @Test
    @DisplayName("CREATED 상태의 OrderResult가 반환되면 CREATED 이력을 저장한다")
    void recordHistory_created_savesCreatedHistory() {
        // Given
        OrderResult result = new OrderResult(
                OrderFixtureValues.ORDER_ID, OrderStatus.CREATED,
                OrderFixtureValues.SKU, OrderFixtureValues.QUANTITY, BigDecimal.valueOf(10000)
        );

        // When
        aspect.recordHistory(result);

        // Then
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        then(orderStatusHistoryRepository).should().save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(OrderFixtureValues.ORDER_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(captor.getValue().getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태의 OrderResult가 반환되면 CONFIRMED 이력을 저장한다")
    void recordHistory_confirmed_savesConfirmedHistory() {
        // Given
        OrderResult result = new OrderResult(
                OrderFixtureValues.ORDER_ID, OrderStatus.CONFIRMED,
                OrderFixtureValues.SKU, OrderFixtureValues.QUANTITY, BigDecimal.valueOf(10000)
        );

        // When
        aspect.recordHistory(result);

        // Then
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        then(orderStatusHistoryRepository).should().save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(OrderFixtureValues.ORDER_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(captor.getValue().getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("CANCELLED 상태의 OrderResult가 반환되면 CANCELLED 이력을 저장한다")
    void recordHistory_cancelled_savesCancelledHistory() {
        // Given
        OrderResult result = new OrderResult(
                OrderFixtureValues.ORDER_ID, OrderStatus.CANCELLED,
                OrderFixtureValues.SKU, OrderFixtureValues.QUANTITY, BigDecimal.valueOf(10000)
        );

        // When
        aspect.recordHistory(result);

        // Then
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        then(orderStatusHistoryRepository).should().save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(OrderFixtureValues.ORDER_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(captor.getValue().getChangedAt()).isNotNull();
    }
}
