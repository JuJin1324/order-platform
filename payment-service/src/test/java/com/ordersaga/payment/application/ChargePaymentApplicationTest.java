package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.Payment;
import com.ordersaga.payment.domain.PaymentRepository;
import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.payment.fixture.ChargePaymentCommandFixture;
import com.ordersaga.payment.infrastructure.InventoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChargePaymentApplicationTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InventoryClient inventoryClient;

    private PaymentApplicationService sut;

    @BeforeEach
    void setUp() {
        sut = new PaymentApplicationService(paymentRepository, inventoryClient);
        given(paymentRepository.save(any(Payment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("재고 차감 성공 시 결제 상태는 COMPLETED")
    void inventorySuccess_paymentStatusCompleted() {
        // Given
        ChargePaymentCommand command = ChargePaymentCommandFixture.normal();
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
        given(inventoryClient.deductInventory(eq(command.sku()), eq(command.quantity()), eq(command.forceInventoryFailure())))
                .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> sut.chargePayment(command))
                .isInstanceOf(IllegalStateException.class);
    }
}
