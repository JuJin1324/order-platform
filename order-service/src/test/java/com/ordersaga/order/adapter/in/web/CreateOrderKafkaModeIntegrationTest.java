package com.ordersaga.order.adapter.in.web;

import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.application.port.out.ChargePaymentPort;
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

@SpringBootTest(properties = "app.saga.mode=kafka")
@AutoConfigureMockMvc
class CreateOrderKafkaModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChargePaymentPort chargePaymentPort;

    @MockitoBean
    private PublishOrderCreatedPort publishOrderCreatedPort;

    @Test
    @DisplayName("Kafka 모드 주문 생성 시 CREATED 상태로 응답하고 order-created 이벤트를 발행한다")
    void createOrder_returnsCreatedAndPublishesEvent() throws Exception {
        // Given
        CreateOrderRequest createOrderRequest = CreateOrderRequestFixture.normal();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isString())
                .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.sku").value(createOrderRequest.sku()))
                .andExpect(jsonPath("$.quantity").value(createOrderRequest.quantity()))
                .andExpect(jsonPath("$.amount").value(createOrderRequest.amount().intValue()));

        ArgumentCaptor<OrderResult> publishedOrderCaptor = ArgumentCaptor.forClass(OrderResult.class);
        then(publishOrderCreatedPort).should().publishOrderCreated(publishedOrderCaptor.capture());
        then(chargePaymentPort).shouldHaveNoInteractions();

        OrderResult publishedCreatedOrder = publishedOrderCaptor.getValue();
        assertThat(publishedCreatedOrder.orderId()).isNotBlank();
        assertThat(publishedCreatedOrder.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(publishedCreatedOrder.sku()).isEqualTo(createOrderRequest.sku());
        assertThat(publishedCreatedOrder.quantity()).isEqualTo(createOrderRequest.quantity());
        assertThat(publishedCreatedOrder.amount()).isEqualByComparingTo(createOrderRequest.amount());
    }
}
