package com.ordersaga.order.adapter.in.web;

import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.application.port.out.PublishOrderCreatedPort;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.fixture.CreateOrderRequestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PublishOrderCreatedPort publishOrderCreatedPort;

    @Test
    @DisplayName("주문 생성 시 CREATED 상태로 즉시 응답하고 order-created 이벤트를 발행한다")
    void createOrder_returnsCreatedAndPublishesEvent() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequestFixture.normal();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isString())
                .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.sku").value(request.sku()))
                .andExpect(jsonPath("$.quantity").value(request.quantity()))
                .andExpect(jsonPath("$.amount").value(request.amount().intValue()));

        ArgumentCaptor<OrderResult> publishedCaptor = ArgumentCaptor.forClass(OrderResult.class);
        then(publishOrderCreatedPort).should().publishOrderCreated(publishedCaptor.capture());

        OrderResult published = publishedCaptor.getValue();
        assertThat(published.orderId()).isNotBlank();
        assertThat(published.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(published.sku()).isEqualTo(request.sku());
        assertThat(published.quantity()).isEqualTo(request.quantity());
        assertThat(published.amount()).isEqualByComparingTo(request.amount());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 응답을 반환한다")
    void missingRequiredField_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateOrderRequestFixture.missingSku())))
                .andExpect(status().isBadRequest());
    }
}
