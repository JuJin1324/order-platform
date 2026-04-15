package com.ordersaga.payment.application;

import com.ordersaga.payment.domain.PaymentRepository;
import com.ordersaga.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ordersaga.payment.fixture.PaymentFixtureValues.AMOUNT;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.ORDER_ID;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.OVER_LIMIT_AMOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentApplicationTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentApplicationService paymentApplicationService;

    @BeforeEach
    void setUp() {
        paymentApplicationService = new PaymentApplicationService(paymentRepository);
    }

    @Test
    @DisplayName("결제 성공 시 COMPLETED 상태의 결제 결과를 반환한다")
    void processPayment_success_returnsCompletedPaymentResult() {
        // Given
        ChargePaymentCommand command = new ChargePaymentCommand(ORDER_ID, AMOUNT, "SKU-001", 2);

        // When
        PaymentResult result = paymentApplicationService.processPayment(command);

        // Then
        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("결제 금액이 한도를 초과하면 예외가 발생한다")
    void processPayment_whenAmountExceedsLimit_throwsIllegalStateException() {
        // Given
        ChargePaymentCommand command = new ChargePaymentCommand(ORDER_ID, OVER_LIMIT_AMOUNT, "SKU-001", 2);

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.processPayment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("payment amount exceeds limit");
    }
}
