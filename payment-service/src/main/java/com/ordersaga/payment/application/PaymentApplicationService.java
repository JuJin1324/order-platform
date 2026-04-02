package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.Payment;
import com.ordersaga.payment.domain.PaymentRepository;
import com.ordersaga.payment.infrastructure.InventoryClient;
import org.springframework.stereotype.Service;

@Service
public class PaymentApplicationService {
    private final PaymentRepository paymentRepository;
    private final InventoryClient inventoryClient;

    public PaymentApplicationService(PaymentRepository paymentRepository, InventoryClient inventoryClient) {
        this.paymentRepository = paymentRepository;
        this.inventoryClient = inventoryClient;
    }

    public PaymentResult chargePayment(ChargePaymentCommand command) {
        Payment payment = Payment.complete(command.orderId(), command.amount());
        paymentRepository.save(payment);

        boolean inventorySuccess = inventoryClient.deductInventory(
                command.sku(), command.quantity(), command.forceInventoryFailure()
        );

        if (!inventorySuccess) {
            throw new IllegalStateException("inventory deduction failed for order: " + command.orderId());
        }

        return PaymentResult.from(payment);
    }
}
