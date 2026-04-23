package com.ordersaga.order.adapter.in.web;

import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.application.port.out.PublishOrderCreatedPort;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.domain.OrderStatusHistoryRepository;
import com.ordersaga.order.fixture.CreateOrderRequestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderStatusHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @MockitoBean
    private PublishOrderCreatedPort publishOrderCreatedPort;

    @BeforeEach
    void setUp() {
        orderStatusHistoryRepository.deleteAll();
    }

    @Test
    @DisplayName("주문 생성 시 CREATED 이력이 1건 기록된다")
    void createOrder_recordsCreatedHistory() throws Exception {
        // Given
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateOrderRequestFixture.normal())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readValue(response, OrderResult.class).orderId();

        // When
        var histories = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId);

        // Then
        assertThat(histories).hasSize(1);
        assertThat(histories.getFirst().getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("GET /status-history 는 주문 생성 후 CREATED 이력을 반환한다")
    void getStatusHistory_returnsCreatedHistory() throws Exception {
        // Given
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateOrderRequestFixture.normal())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readValue(response, OrderResult.class).orderId();

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}/status-history", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$[0].changedAt").isString());
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID 조회 시 빈 배열을 반환한다")
    void getStatusHistory_unknownOrderId_returnsEmptyArray() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders/non-existent-id/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
