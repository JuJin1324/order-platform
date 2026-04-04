package com.ordersaga.payment.presentation;

import java.util.Map;

import com.ordersaga.payment.application.ChargePaymentCommand;
import com.ordersaga.payment.application.PaymentProcessor;
import com.ordersaga.payment.application.PaymentResult;
import com.ordersaga.payment.presentation.dto.ChargePaymentRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
public class PaymentController {
    private final PaymentProcessor paymentProcessor;

    public PaymentController(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "payment-service",
                "status", "ok"
        );
    }

    @PostMapping
    public PaymentResult chargePayment(@Valid @RequestBody ChargePaymentRequest request) {
        ChargePaymentCommand command = new ChargePaymentCommand(
                request.orderId(),
                request.amount(),
                request.sku(),
                request.quantity(),
                request.forceInventoryFailure()
        );
        return paymentProcessor.chargePayment(command);
    }
}
