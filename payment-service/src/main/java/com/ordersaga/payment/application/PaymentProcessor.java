package com.ordersaga.payment.application;

import com.ordersaga.payment.infrastructure.InventoryClient;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessor {
    private final PaymentApplicationService paymentApplicationService;
    private final InventoryClient inventoryClient;

    public PaymentProcessor(PaymentApplicationService paymentApplicationService, InventoryClient inventoryClient) {
        this.paymentApplicationService = paymentApplicationService;
        this.inventoryClient = inventoryClient;
    }

    public PaymentResult chargePayment(ChargePaymentCommand command) {
        PaymentResult result = paymentApplicationService.processPayment(command);

        boolean inventorySuccess = inventoryClient.deductInventory(
                command.sku(), command.quantity(), command.forceInventoryFailure()
        );

        if (!inventorySuccess) {
            throw new IllegalStateException("inventory deduction failed for order: " + command.orderId());
        }

        return result;
    }
}
