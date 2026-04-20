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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("필수 필드 누락 시 400 응답과 VALIDATION_FAILED 코드를 반환한다")
    void missingRequiredField_returns400WithErrorResponse() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateOrderRequestFixture.missingSku())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("존재하지 않는 주문 조회 시 404 응답과 ORDER_NOT_FOUND 코드를 반환한다")
    void getOrder_notFound_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/api/orders/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }
}
