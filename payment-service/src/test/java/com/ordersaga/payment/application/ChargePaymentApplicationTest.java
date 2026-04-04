package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.payment.fixture.ChargePaymentCommandFixture;
import com.ordersaga.payment.fixture.PaymentFixtureValues;
import com.ordersaga.payment.infrastructure.InventoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChargePaymentApplicationTest {

    @Mock
    private PaymentApplicationService paymentApplicationService;

    @Mock
    private InventoryClient inventoryClient;

    private PaymentProcessor sut;

    private static final String PAYMENT_ID = "test-payment-id";

    @BeforeEach
    void setUp() {
        sut = new PaymentProcessor(paymentApplicationService, inventoryClient);
    }

    @Test
    @DisplayName("재고 차감 성공 시 결제 상태는 COMPLETED")
    void inventorySuccess_paymentStatusCompleted() {
        // Given
        ChargePaymentCommand command = ChargePaymentCommandFixture.normal();
        PaymentResult paymentResult = new PaymentResult(PAYMENT_ID, command.orderId(), PaymentStatus.COMPLETED, command.amount());

        given(paymentApplicationService.processPayment(command)).willReturn(paymentResult);
        given(inventoryClient.deductInventory(eq(command.sku()), eq(command.quantity()), eq(command.forceInventoryFailure())))
                .willReturn(true);

        // When
        PaymentResult result = sut.chargePayment(command);

        // Then
        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.orderId()).isEqualTo(command.orderId());
    }

    @Test
    @DisplayName("재고 차감 실패 시 예외가 발생한다")
    void inventoryFailure_throwsException() {
        // Given
        ChargePaymentCommand command = ChargePaymentCommandFixture.withForceInventoryFailure();
        PaymentResult paymentResult = new PaymentResult(PAYMENT_ID, command.orderId(), PaymentStatus.COMPLETED, command.amount());

        given(paymentApplicationService.processPayment(command)).willReturn(paymentResult);
        given(inventoryClient.deductInventory(eq(command.sku()), eq(command.quantity()), eq(command.forceInventoryFailure())))
                .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> sut.chargePayment(command))
                .isInstanceOf(IllegalStateException.class);
    }
}
