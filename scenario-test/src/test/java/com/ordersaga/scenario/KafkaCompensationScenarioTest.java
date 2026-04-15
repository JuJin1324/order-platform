package com.ordersaga.scenario;

import com.ordersaga.inventory.InventoryServiceApplication;
import com.ordersaga.inventory.domain.Inventory;
import com.ordersaga.inventory.domain.InventoryRepository;
import com.ordersaga.order.OrderServiceApplication;
import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderRepository;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.payment.PaymentServiceApplication;
import com.ordersaga.payment.domain.Payment;
import com.ordersaga.payment.domain.PaymentRepository;
import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.saga.SagaTopics;
import com.ordersaga.scenario.fixture.CreateOrderRequestFixture;
import com.ordersaga.scenario.fixture.ScenarioFixtureValues;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.web.client.RestClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaCompensationScenarioTest {
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:3.8.0");
    private static final Duration SAGA_COMPLETION_TIMEOUT = Duration.ofSeconds(15);

    private KafkaContainer kafkaContainer;

    private ConfigurableApplicationContext inventoryContext;
    private ConfigurableApplicationContext paymentContext;
    private ConfigurableApplicationContext orderContext;

    private InventoryRepository inventoryRepository;
    private PaymentRepository paymentRepository;
    private OrderRepository orderRepository;
    private RestClient orderClient;

    @BeforeAll
    void startInfraAndServices() {
        kafkaContainer = new KafkaContainer(KAFKA_IMAGE);
        kafkaContainer.start();

        try {
            String bootstrapServers = kafkaContainer.getBootstrapServers();
            createTopics(bootstrapServers);

            inventoryContext = new SpringApplicationBuilder(InventoryServiceApplication.class)
                    .run(serviceArgs("inventory-kafka-compensation-db", bootstrapServers));

            paymentContext = new SpringApplicationBuilder(PaymentServiceApplication.class)
                    .run(serviceArgs("payment-kafka-compensation-db", bootstrapServers));

            orderContext = new SpringApplicationBuilder(OrderServiceApplication.class)
                    .run(serviceArgs("order-kafka-compensation-db", bootstrapServers));
            int orderPort = localPort(orderContext);

            inventoryRepository = inventoryContext.getBean(InventoryRepository.class);
            paymentRepository = paymentContext.getBean(PaymentRepository.class);
            orderRepository = orderContext.getBean(OrderRepository.class);
            orderClient = RestClient.builder()
                    .baseUrl("http://localhost:" + orderPort)
                    .build();

            waitForListenerAssignments(orderContext);
            waitForListenerAssignments(paymentContext);
            waitForListenerAssignments(inventoryContext);
        } catch (RuntimeException | Error e) {
            stopInfraAndServices();
            throw e;
        }
    }

    @AfterAll
    void stopInfraAndServices() {
        closeContext(orderContext);
        orderContext = null;

        closeContext(paymentContext);
        paymentContext = null;

        closeContext(inventoryContext);
        inventoryContext = null;

        if (kafkaContainer != null) {
            kafkaContainer.stop();
            kafkaContainer = null;
        }
    }

    @BeforeEach
    void resetState() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryRepository.deleteAll();
        inventoryRepository.save(new Inventory(
                ScenarioFixtureValues.SKU,
                ScenarioFixtureValues.INITIAL_INVENTORY_QUANTITY
        ));
    }

    @Test
    @DisplayName("시나리오 A — 결제 실패 시 주문이 CANCELLED로 수렴한다")
    void scenarioA_paymentFailure_cancelsOrder() {
        // Given: 결제 한도를 초과하는 금액으로 주문
        CreateOrderRequest request = CreateOrderRequestFixture.overLimitAmount();

        // When
        OrderResult createdOrder = createOrder(request);
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.CREATED);

        // Then
        await().atMost(SAGA_COMPLETION_TIMEOUT).untilAsserted(() -> {
            assertThat(orderRepository.findByOrderId(createdOrder.orderId()))
                    .isPresent()
                    .get()
                    .extracting(Order::getStatus)
                    .isEqualTo(OrderStatus.CANCELLED);

            assertThat(paymentRepository.findByOrderId(createdOrder.orderId()))
                    .isEmpty();
        });
    }

    @Test
    @DisplayName("시나리오 B — 재고 부족 시 결제가 CANCELED되고 주문이 CANCELLED로 수렴한다")
    void scenarioB_inventoryFailure_cancelsPaymentAndOrder() {
        // Given: 재고를 0으로 설정
        inventoryRepository.deleteAll();
        inventoryRepository.save(new Inventory(ScenarioFixtureValues.SKU, 0));

        CreateOrderRequest request = CreateOrderRequestFixture.normal();

        // When
        OrderResult createdOrder = createOrder(request);
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.CREATED);

        // Then
        await().atMost(SAGA_COMPLETION_TIMEOUT).untilAsserted(() -> {
            assertThat(orderRepository.findByOrderId(createdOrder.orderId()))
                    .isPresent()
                    .get()
                    .extracting(Order::getStatus)
                    .isEqualTo(OrderStatus.CANCELLED);

            assertThat(paymentRepository.findByOrderId(createdOrder.orderId()))
                    .isPresent()
                    .get()
                    .extracting(Payment::getStatus)
                    .isEqualTo(PaymentStatus.CANCELED);
        });
    }

    private OrderResult createOrder(CreateOrderRequest request) {
        return orderClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OrderResult.class);
    }

    private void createTopics(String bootstrapServers) {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
        ))) {
            adminClient.createTopics(List.of(
                    new NewTopic(SagaTopics.ORDER_CREATED, 1, (short) 1),
                    new NewTopic(SagaTopics.PAYMENT_COMPLETED, 1, (short) 1),
                    new NewTopic(SagaTopics.INVENTORY_DEDUCTED, 1, (short) 1),
                    new NewTopic(SagaTopics.INVENTORY_DEDUCTION_FAILED, 1, (short) 1),
                    new NewTopic(SagaTopics.PAYMENT_FAILED, 1, (short) 1),
                    new NewTopic(SagaTopics.PAYMENT_CANCELLED, 1, (short) 1)
            )).all().get();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create Kafka topics for scenario test", exception);
        }
    }

    private String[] serviceArgs(String databaseName, String bootstrapServers) {
        String[] commonArgs = new String[]{
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.sql.init.mode=never",
                "--logging.level.root=WARN",
                "--logging.level.org.springframework=WARN",
                "--logging.level.org.hibernate.SQL=OFF",
                "--logging.level.org.hibernate.orm.jdbc.bind=OFF"
        };
        String[] extraArgs = new String[]{"--spring.kafka.bootstrap-servers=" + bootstrapServers};

        String[] mergedArgs = new String[commonArgs.length + extraArgs.length];
        System.arraycopy(commonArgs, 0, mergedArgs, 0, commonArgs.length);
        System.arraycopy(extraArgs, 0, mergedArgs, commonArgs.length, extraArgs.length);
        return mergedArgs;
    }

    private void waitForListenerAssignments(ConfigurableApplicationContext context) {
        KafkaListenerEndpointRegistry registry = context.getBean(KafkaListenerEndpointRegistry.class);
        await().atMost(SAGA_COMPLETION_TIMEOUT).untilAsserted(() ->
                assertThat(registry.getListenerContainers())
                        .allSatisfy(container -> assertThat(container.getAssignedPartitions()).isNotEmpty())
        );
    }

    private int localPort(ConfigurableApplicationContext context) {
        return context.getEnvironment().getProperty("local.server.port", Integer.class, 0);
    }

    private void closeContext(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }
}
