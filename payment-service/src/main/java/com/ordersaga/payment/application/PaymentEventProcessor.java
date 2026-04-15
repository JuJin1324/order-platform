package com.ordersaga.payment.application;

import com.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.ordersaga.saga.event.InventoryDeductionFailedEvent;
import com.ordersaga.saga.event.OrderCreatedEvent;
import com.ordersaga.saga.event.PaymentCancelledEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import com.ordersaga.saga.event.PaymentFailedEvent;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProcessor {
    private final PaymentApplicationService paymentApplicationService;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentEventProcessor(
            PaymentApplicationService paymentApplicationService,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentApplicationService = paymentApplicationService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    public void handleOrderCreated(OrderCreatedEvent event) {
        ChargePaymentCommand command = new ChargePaymentCommand(
                event.orderId(),
                event.amount(),
                event.sku(),
                event.quantity()
        );
        try {
            PaymentResult paymentResult = paymentApplicationService.processPayment(command);
            paymentEventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                    paymentResult.orderId(),
                    paymentResult.paymentId(),
                    paymentResult.amount(),
                    event.sku(),
                    event.quantity()
            ));
        } catch (IllegalStateException e) {
            paymentEventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                    event.orderId(),
                    e.getMessage()
            ));
        }
    }

    public void handleInventoryDeductionFailed(InventoryDeductionFailedEvent event) {
        PaymentResult result = paymentApplicationService.cancelPayment(event.orderId());
        paymentEventPublisher.publishPaymentCancelled(new PaymentCancelledEvent(
                event.orderId(),
                result.paymentId()
        ));
    }
}
