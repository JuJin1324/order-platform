package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.Payment;
import com.ordersaga.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApplicationService {
    private final PaymentRepository paymentRepository;

    public PaymentApplicationService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResult processPayment(ChargePaymentCommand command) {
        Payment payment = Payment.complete(command.orderId(), command.amount());
        paymentRepository.save(payment);
        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult cancelPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("payment not found for orderId: " + orderId));
        payment.cancel();
        return PaymentResult.from(payment);
    }
}
