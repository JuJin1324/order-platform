# Kafka 애플리케이션 설정 — Step 2b

## 1. 개요

이 문서는 Spring 애플리케이션에서 Kafka를 사용하기 위한 설정을 다룬다.
의존성 구성, `application.yml`, Java Configuration, 공유 이벤트 스키마, Publisher/Listener 구현 패턴을 정리한다.
Kafka 브로커 자체의 구성(docker-compose, 클러스터 설정 등)은 `kafka-infra.md`를 참고한다.

---

## 2. 의존성

루트 `build.gradle`에서 `saga-events`를 제외한 모든 서비스 모듈에 Spring Boot가 적용된다.
각 서비스 `build.gradle`에는 아래 두 의존성이 추가된다.

```groovy
// 예: order-service/build.gradle
dependencies {
    implementation project(':saga-events')   // 공유 이벤트 스키마
    implementation 'org.springframework.kafka:spring-kafka'
}
```

`spring-kafka` 버전은 루트의 `io.spring.dependency-management`가 Spring Boot BOM을 통해 자동으로 관리한다.
세 서비스가 동일한 버전의 `spring-kafka`를 사용하도록 강제하여, 직렬화 호환성 문제가 생기지 않게 한다.

Spring Boot의 Kafka 자동 설정(`KafkaAutoConfiguration`)은 Spring Boot 4.x에서 `spring-boot-kafka` 모듈 안에 있다.
이 프로젝트는 `org.springframework.boot:spring-boot-kafka`가 아닌 `org.springframework.kafka:spring-kafka`에만 의존하기 때문에
`KafkaAutoConfiguration` 클래스 자체가 classpath에 존재하지 않아 자동 설정이 동작하지 않는다.
따라서 아래 섹션에서 설명하는 Java Configuration으로 빈을 직접 선언하는 것이 필수다.

---

## 3. application.yml — 공통 Kafka 설정

세 서비스의 설정 구조는 동일하며, `spring.application.name`만 다르다.

```yaml
spring:
  application:
    name: order-service          # 서비스별로 다름 (payment-service, inventory-service)
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: com.ordersaga.saga.event
```

| 항목 | 값 | 의도 |
|---|---|---|
| `bootstrap-servers` | `localhost:9092` (기본값) | 로컬 개발 환경에서는 기본값을 그대로 쓰고, 배포 환경에서는 환경변수 `KAFKA_BOOTSTRAP_SERVERS`로 오버라이드한다. 코드 수정 없이 환경을 전환할 수 있다. |
| `consumer.group-id` | 서비스 이름과 동일 | Consumer group이 다르면 같은 토픽을 여러 서비스가 독립적으로 소비할 수 있다. 서비스 이름을 그대로 쓰면 group ID가 유일하게 보장되고, 설정값이 두 곳에서 관리되는 것을 피할 수 있다. |
| `auto-offset-reset` | `earliest` | Saga에서 이벤트를 하나라도 놓치면 상태 전이가 멈춘다. 서비스가 처음 뜨거나 offset이 만료된 경우, 토픽의 가장 오래된 메시지부터 소비하게 하여 누락을 방지한다. 단, 이미 offset을 커밋한 이력이 있는 서비스가 재시작할 때는 `earliest`여도 커밋된 offset부터 이어서 읽으므로 중복 소비가 발생하지 않는다. |
| `trusted.packages` | `com.ordersaga.saga.event` | `JacksonJsonDeserializer`는 기본적으로 임의 클래스로의 역직렬화를 거부한다. 허용 패키지를 이벤트 모듈로만 좁혀 신뢰되지 않은 클래스가 인스턴스화되는 역직렬화 공격면을 최소화한다. |

---

## 4. Java Configuration — KafkaConfiguration

각 서비스마다 `*KafkaConfiguration` 클래스가 존재하며 구조는 동일하다.
(`OrderKafkaConfiguration`, `PaymentKafkaConfiguration`, `InventoryKafkaConfiguration`)

### 4-1. Producer 설정

```java
@Bean
ProducerFactory<Object, Object> producerFactory(Environment environment) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            environment.getRequiredProperty("spring.kafka.bootstrap-servers"));
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(properties);
}

@Bean
KafkaOperations<Object, Object> kafkaOperations(ProducerFactory<Object, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
}
```

- **Key를 `String`(orderId)으로 지정**: Kafka는 같은 key를 가진 메시지를 항상 같은 파티션으로 보낸다. orderId를 key로 쓰면 한 주문에 관한 이벤트가 순서대로 처리되는 것이 보장된다.
- **Value를 `JacksonJsonSerializer`로 지정**: `saga-events`의 record 타입을 JSON으로 직렬화한다. 바이너리 포맷 없이 JSON을 그대로 쓰므로 Kafka 콘솔에서 메시지 내용을 육안으로 확인할 수 있다.
- **`KafkaTemplate` 대신 `KafkaOperations` 인터페이스로 노출**: Publisher 컴포넌트가 구체 클래스가 아닌 인터페이스에만 의존하게 하여, 테스트에서 Mock으로 교체하거나 다른 구현체로 전환할 때 Publisher 코드를 건드리지 않아도 된다.

### 4-2. Consumer 설정

```java
@Bean
ConsumerFactory<Object, Object> consumerFactory(Environment environment) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ...);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG,
            environment.getRequiredProperty("spring.kafka.consumer.group-id"));
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            environment.getProperty("spring.kafka.consumer.auto-offset-reset", "earliest"));
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,
            environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages",
                    "com.ordersaga.saga.event"));
    return new DefaultKafkaConsumerFactory<>(properties);
}

@Bean
ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
        ConsumerFactory<Object, Object> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
}
```

- **`JacksonJsonDeserializer` 사용**: `@KafkaListener` 메서드의 파라미터 타입 선언만으로 JSON → 이벤트 record 변환이 자동으로 이루어진다. Listener 코드에서 역직렬화 로직을 직접 작성할 필요가 없다.
- **`AUTO_OFFSET_RESET`의 기본값을 `"earliest"`로 하드코딩**: yml에서 값이 누락되더라도 안전한 쪽으로 동작하게 방어한다.

---

## 5. 공유 이벤트 스키마 (`saga-events` 모듈)

### 5-1. 토픽 상수 — `SagaTopics`

```java
// saga-events/src/main/java/com/ordersaga/saga/SagaTopics.java
public final class SagaTopics {
    public static final String ORDER_CREATED     = "order-created";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String INVENTORY_DEDUCTED = "inventory-deducted";
}
```

토픽 이름을 발행자와 구독자가 각각 문자열 리터럴로 선언하면, 한쪽에서 오타가 나더라도 컴파일 에러 없이 조용히 연결이 끊긴다.
`SagaTopics`에 상수를 모아두고 양쪽이 이 상수를 참조하게 하면, 토픽 이름 변경이 필요할 때 한 곳만 수정하면 되고 누락된 쪽은 컴파일이 실패하여 바로 드러난다.

### 5-2. 이벤트 레코드

| 클래스 | 토픽 | 필드 |
|---|---|---|
| `OrderCreatedEvent` | `order-created` | `orderId`, `sku`, `quantity`, `amount` |
| `PaymentCompletedEvent` | `payment-completed` | `orderId`, `paymentId`, `amount`, `sku`, `quantity` |
| `InventoryDeductedEvent` | `inventory-deducted` | `orderId`, `sku`, `deductedQuantity`, `remainingQuantity` |

이벤트를 Java `record`로 선언한 이유는 두 가지다.
첫째, record는 불변이므로 수신자가 이벤트 객체를 변형할 수 없다 — 이벤트는 "이미 일어난 사실"이어야 하기 때문이다.
둘째, Jackson은 record의 컴포넌트를 자동으로 직렬화/역직렬화하므로 별도의 매핑 코드가 필요 없다.

`PaymentCompletedEvent`에 `sku`와 `quantity`가 포함된 이유는, inventory-service가 재고를 차감할 때 어떤 상품을 얼마나 차감해야 하는지 알아야 하기 때문이다.
이 값을 별도로 조회하지 않고 이벤트에 포함시켜 서비스 간 추가 통신 없이 처리가 완결되도록 설계했다.

---

## 6. 토픽별 발행/구독 매핑

```
order-service          payment-service        inventory-service
──────────────         ───────────────        ─────────────────
[order-created] ──→    onOrderCreated()
                       [payment-completed] ──→ onPaymentCompleted()
                                               [inventory-deducted] ──→ onInventoryDeducted()
                                                                         (order-service가 구독)
```

| 토픽 | 발행 서비스 | 구독 서비스 | 처리 내용 |
|---|---|---|---|
| `order-created` | order-service | payment-service | 결제 처리 시작 |
| `payment-completed` | payment-service | inventory-service | 재고 차감 시작 |
| `inventory-deducted` | inventory-service | order-service | 주문 상태 `CONFIRMED` 전환 |

이 흐름은 Choreography 방식의 Saga다.
중앙에서 흐름을 지시하는 Orchestrator 없이, 각 서비스가 자신이 구독하는 이벤트에 반응하여 다음 단계를 스스로 진행한다.
각 서비스는 "내 앞 단계가 성공했다"는 이벤트만 알면 되므로, 다른 서비스의 내부 구조를 알 필요가 없다.

---

## 7. Publisher / Listener 구현 패턴

### Publisher — `KafkaOperations.send(topic, key, value)`

```java
kafkaOperations.send(SagaTopics.ORDER_CREATED, event.orderId(), event);
```

key에 `orderId`를 사용하는 것이 핵심이다.
Kafka는 같은 key의 메시지를 같은 파티션으로 보내고, 파티션 안에서는 순서를 보장한다.
따라서 하나의 orderId에 대한 이벤트 — order-created → payment-completed → inventory-deducted — 가 반드시 순서대로 처리된다.

### Listener — `@KafkaListener`

```java
@KafkaListener(topics = SagaTopics.INVENTORY_DEDUCTED, groupId = "${spring.application.name}")
public void onInventoryDeducted(InventoryDeductedEvent event) {
    orderEventProcessor.handleInventoryDeducted(event);
}
```

- **`groupId = "${spring.application.name}"`**: `application.yml`의 `consumer.group-id` 값과 일치시킨다. Listener 어노테이션의 groupId와 yml 설정이 따로 놀면 의도하지 않은 별개의 consumer group이 생긴다.
- **Listener는 이벤트 수신만 담당**: 비즈니스 로직은 `EventProcessor`에 위임하여, Kafka 인프라 계층과 유스케이스 로직이 섞이지 않게 한다. Listener는 얇게 유지해야 비즈니스 로직을 Kafka 없이 단위 테스트할 수 있다.

---

## 8. 운영 환경에서 추가로 설정해야 할 항목

현재 애플리케이션 설정은 로컬 개발 편의에 맞춰져 있다. 운영 환경에서는 아래 항목들을 반드시 검토해야 한다.

### Producer — `acks=all`

현재 Producer 설정에는 `acks`가 명시되어 있지 않아 기본값(`1`, 리더에만 기록 확인)이 적용된다.
Saga에서 이벤트가 유실되면 상태 전이가 멈추기 때문에, 운영에서는 `acks=all`로 모든 동기 복제본에 기록된 것을 확인한 뒤 발행 성공으로 처리해야 한다.
재시도 설정도 함께 추가하여 일시적인 브로커 응답 지연에 대비한다.

```java
properties.put(ProducerConfig.ACKS_CONFIG, "all");
properties.put(ProducerConfig.RETRIES_CONFIG, 3);
properties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
```

> **브로커와의 짝 설정**: `acks=all`은 브로커/토픽의 `min.insync.replicas`와 함께 설정해야 의미가 완성된다. `acks=all`만 설정하고 `min.insync.replicas`가 1이면, 복제본이 1개뿐인 것을 확인하고도 성공으로 처리한다. 브로커 설정은 `kafka-infra.md`를 참고한다.

### Consumer — 수동 커밋 (`enable.auto.commit=false`)

현재 Consumer는 `enable.auto.commit` 기본값(`true`, 자동 커밋)을 따른다.
자동 커밋은 메시지를 가져온 직후 offset을 커밋하므로, 처리 도중 서비스가 죽으면 메시지를 소비한 것으로 기록되어 재처리 기회를 잃는다.
운영에서는 비즈니스 로직 처리 완료 후 수동으로 offset을 커밋하도록 설정해야 한다.

```java
// ConsumerFactory
properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

// ConcurrentKafkaListenerContainerFactory
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
```

`MANUAL_IMMEDIATE`를 사용하면 `@KafkaListener` 메서드에서 `Acknowledgment.acknowledge()`를 명시적으로 호출해야 offset이 커밋된다.

```java
@KafkaListener(topics = SagaTopics.ORDER_CREATED, groupId = "${spring.application.name}")
public void onOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
    paymentEventProcessor.handleOrderCreated(event);
    ack.acknowledge();  // 처리 완료 후에만 커밋
}
```

**수동 커밋 도입 시 멱등성이 필요하다.**
처리는 성공했지만 `ack.acknowledge()` 직전에 서비스가 죽으면, 재시작 후 같은 메시지를 다시 소비한다.
이때 `auto-offset-reset: earliest` 설정과 맞물려, offset 자체가 없는 상황(신규 서비스 배포, offset 보존 기간 만료 등)이라면 이미 처리한 메시지까지 처음부터 다시 읽게 된다.

> Kafka의 기본 offset 보존 기간(`offsets.retention.minutes`)은 7일이다. 서비스가 7일 이상 내려가 있다가 재시작하면 저장된 offset이 사라져 `earliest`부터 재소비가 시작된다.

따라서 수동 커밋을 적용하면 각 서비스의 이벤트 처리 로직에 **멱등성(idempotency)** 을 함께 확보해야 한다.
같은 이벤트가 두 번 들어와도 결과가 달라지지 않도록, 예를 들어 처리 전 이미 처리된 `orderId`인지 확인하는 방어 로직이 필요하다.

### 보안 — SASL 인증 (+ TLS는 상황에 따라)

현재는 암호화와 인증이 모두 비활성화된 PLAINTEXT로 연결된다. 운영 환경에서는 최소한 SASL 인증을 추가해야 한다.

TLS 암호화는 네트워크 경계와 규정에 따라 판단한다. 내부망이 확실히 보장되고 규정상 전송 암호화 요건이 없다면 SASL만 적용하는 것도 합리적인 선택이다. TLS를 함께 써야 하는 기준은 `kafka-infra.md`를 참고한다.

#### SASL만 적용하는 경우 (내부망, TLS 생략)

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_PLAINTEXT
    properties:
      sasl.mechanism: SCRAM-SHA-256
      sasl.jaas.config: >
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
```

#### TLS + SASL을 함께 적용하는 경우

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    security:
      protocol: SASL_SSL
    properties:
      sasl.mechanism: SCRAM-SHA-256
      sasl.jaas.config: >
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
    ssl:
      trust-store-location: classpath:kafka.truststore.jks
      trust-store-password: ${KAFKA_TRUSTSTORE_PASSWORD}
```

> **브로커와의 짝 설정**: 애플리케이션의 `security.protocol`은 브로커의 리스너 프로토콜과 반드시 일치해야 한다. `SASL_PLAINTEXT`로 설정했는데 브로커가 `SASL_SSL`만 열어 두면 접속이 거부된다. 브로커 설정은 `kafka-infra.md`를 참고한다.
