package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.ordersaga.saga.event.OrderCreatedEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ordersaga.payment.fixture.PaymentFixtureValues.AMOUNT;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.ORDER_ID;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.PAYMENT_ID;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.QUANTITY;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.SKU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PaymentEventProcessorTest {

    @Mock
    private PaymentApplicationService paymentApplicationService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentEventProcessor paymentEventProcessor;

    @BeforeEach
    void setUp() {
        paymentEventProcessor = new PaymentEventProcessor(paymentApplicationService, paymentEventPublisher);
    }

    @Test
    @DisplayName("order-created 이벤트를 결제로 저장하고 payment-completed 이벤트를 발행한다")
    void handleOrderCreated_processesPaymentAndPublishesNextEvent() {
        // Given
        OrderCreatedEvent receivedEvent = receivedOrderCreatedEvent();
        PaymentResult completedPayment = expectedCompletedPayment();

        given(paymentApplicationService.processPayment(any(ChargePaymentCommand.class)))
                .willReturn(completedPayment);

        // When
        PaymentResult result = paymentEventProcessor.handleOrderCreated(receivedEvent);

        // Then
        assertThat(result).isEqualTo(completedPayment);
        then(paymentApplicationService).should().processPayment(expectedChargePaymentCommand(receivedEvent));
        then(paymentEventPublisher).should().publishPaymentCompleted(expectedPaymentCompletedEvent(receivedEvent));
    }

    private OrderCreatedEvent receivedOrderCreatedEvent() {
        return new OrderCreatedEvent(ORDER_ID, SKU, QUANTITY, AMOUNT);
    }

    private ChargePaymentCommand expectedChargePaymentCommand(OrderCreatedEvent receivedOrderCreatedEvent) {
        return new ChargePaymentCommand(
                receivedOrderCreatedEvent.orderId(),
                receivedOrderCreatedEvent.amount(),
                receivedOrderCreatedEvent.sku(),
                receivedOrderCreatedEvent.quantity(),
                false
        );
    }

    private PaymentResult expectedCompletedPayment() {
        return new PaymentResult(PAYMENT_ID, ORDER_ID, PaymentStatus.COMPLETED, AMOUNT);
    }

    private PaymentCompletedEvent expectedPaymentCompletedEvent(OrderCreatedEvent receivedOrderCreatedEvent) {
        return new PaymentCompletedEvent(
                receivedOrderCreatedEvent.orderId(),
                PAYMENT_ID,
                receivedOrderCreatedEvent.amount(),
                receivedOrderCreatedEvent.sku(),
                receivedOrderCreatedEvent.quantity()
        );
    }
}
