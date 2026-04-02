package com.ordersaga.inventory.presentation;

import com.ordersaga.inventory.fixture.DeductInventoryRequestFixture;
import com.ordersaga.inventory.fixture.InventoryFixtureValues;
import com.ordersaga.inventory.presentation.dto.DeductInventoryRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeductInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("재고 차감 성공 시 남은 수량을 응답한다")
    void deductSuccess_returnsRemainingQuantity() throws Exception {
        // Given
        DeductInventoryRequest request = DeductInventoryRequestFixture.normal();

        // When & Then
        mockMvc.perform(post("/internal/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(request.sku()))
                .andExpect(jsonPath("$.deductedQuantity").value(request.quantity()))
                .andExpect(jsonPath("$.remainingQuantity").value(InventoryFixtureValues.REMAINING_QUANTITY));
    }

    @Test
    @DisplayName("강제 실패 요청 시 예외가 발생한다")
    void forceFailure_throwsException() {
        // Given
        DeductInventoryRequest request = DeductInventoryRequestFixture.withForceFailure();

        // When & Then
        assertThatThrownBy(() ->
                mockMvc.perform(post("/internal/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        ).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 응답을 반환한다")
    void missingRequiredField_returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/internal/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(DeductInventoryRequestFixture.missingSku())))
                .andExpect(status().isBadRequest());
    }
}
