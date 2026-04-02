package com.ordersaga.payment.presentation;

import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.payment.fixture.ChargePaymentRequestFixture;
import com.ordersaga.payment.infrastructure.InventoryClient;
import com.ordersaga.payment.presentation.dto.ChargePaymentRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChargePaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryClient inventoryClient;

    @Test
    @DisplayName("재고 차감 성공 시 COMPLETED 상태로 응답한다")
    void inventorySuccess_returnsCompleted() throws Exception {
        // Given
        ChargePaymentRequest request = ChargePaymentRequestFixture.normal();
        given(inventoryClient.deductInventory(any(), anyInt(), anyBoolean())).willReturn(true);

        // When & Then
        mockMvc.perform(post("/internal/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(request.orderId()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.amount").value(request.amount().intValue()));
    }

    @Test
    @DisplayName("재고 차감 실패 시 예외가 발생한다")
    void inventoryFailure_throwsException() {
        // Given
        ChargePaymentRequest request = ChargePaymentRequestFixture.withForceInventoryFailure();
        given(inventoryClient.deductInventory(any(), anyInt(), anyBoolean())).willReturn(false);

        // When & Then
        assertThatThrownBy(() ->
                mockMvc.perform(post("/internal/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        ).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 응답을 반환한다")
    void missingRequiredField_returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/internal/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ChargePaymentRequestFixture.missingOrderId())))
                .andExpect(status().isBadRequest());
    }
}
