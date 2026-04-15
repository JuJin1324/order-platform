package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.ordersaga.saga.event.InventoryDeductionFailedEvent;
import com.ordersaga.saga.event.OrderCreatedEvent;
import com.ordersaga.saga.event.PaymentCancelledEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import com.ordersaga.saga.event.PaymentFailedEvent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
    @DisplayName("결제 성공 시 payment-completed 이벤트를 발행한다")
    void handleOrderCreated_processesPaymentAndPublishesNextEvent() {
        // Given
        OrderCreatedEvent receivedEvent = receivedOrderCreatedEvent();
        PaymentResult completedPayment = expectedCompletedPayment();

        given(paymentApplicationService.processPayment(any(ChargePaymentCommand.class)))
                .willReturn(completedPayment);

        // When
        paymentEventProcessor.handleOrderCreated(receivedEvent);

        // Then
        then(paymentApplicationService).should().processPayment(expectedChargePaymentCommand(receivedEvent));
        then(paymentEventPublisher).should().publishPaymentCompleted(expectedPaymentCompletedEvent(receivedEvent));
        then(paymentEventPublisher).should(never()).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("결제 실패 시 payment-failed 이벤트를 발행한다")
    void handleOrderCreated_whenPaymentFails_publishesPaymentFailedEvent() {
        // Given
        OrderCreatedEvent receivedEvent = receivedOrderCreatedEvent();
        String failureReason = "payment processing failed";

        given(paymentApplicationService.processPayment(any(ChargePaymentCommand.class)))
                .willThrow(new IllegalStateException(failureReason));

        // When
        paymentEventProcessor.handleOrderCreated(receivedEvent);

        // Then
        then(paymentEventPublisher).should().publishPaymentFailed(
                new PaymentFailedEvent(ORDER_ID, failureReason)
        );
        then(paymentEventPublisher).should(never()).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("inventory-deduction-failed 수신 시 결제를 취소하고 payment-cancelled 이벤트를 발행한다")
    void handleInventoryDeductionFailed_cancelPaymentAndPublishesCancelledEvent() {
        // Given
        InventoryDeductionFailedEvent receivedEvent = new InventoryDeductionFailedEvent(
                ORDER_ID, SKU, "not enough inventory for sku: " + SKU
        );
        PaymentResult cancelledPayment = new PaymentResult(PAYMENT_ID, ORDER_ID, PaymentStatus.CANCELED, AMOUNT);

        given(paymentApplicationService.cancelPayment(ORDER_ID))
                .willReturn(cancelledPayment);

        // When
        paymentEventProcessor.handleInventoryDeductionFailed(receivedEvent);

        // Then
        then(paymentApplicationService).should().cancelPayment(ORDER_ID);
        then(paymentEventPublisher).should().publishPaymentCancelled(
                new PaymentCancelledEvent(ORDER_ID, PAYMENT_ID)
        );
    }

    private OrderCreatedEvent receivedOrderCreatedEvent() {
        return new OrderCreatedEvent(ORDER_ID, SKU, QUANTITY, AMOUNT);
    }

    private ChargePaymentCommand expectedChargePaymentCommand(OrderCreatedEvent receivedOrderCreatedEvent) {
        return new ChargePaymentCommand(
                receivedOrderCreatedEvent.orderId(),
                receivedOrderCreatedEvent.amount(),
                receivedOrderCreatedEvent.sku(),
                receivedOrderCreatedEvent.quantity()
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
