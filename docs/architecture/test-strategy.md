# 테스트 전략

이 문서는 Order Saga MVP의 레이어별·서비스별 테스트 구조를 정의한다.
새 서비스나 레이어를 추가할 때 이 문서를 기준으로 어떤 테스트가 필요한지 판단한다.

---

## 테스트 레이어 정의

### 단위 테스트 — ApplicationService

**검증 대상**: 도메인 상태 전이가 올바르게 수행되고, repository가 호출되는지

- Spring 컨텍스트 없이 Mockito만 사용한다
- Repository를 mock하고, 도메인 객체는 실제 인스턴스를 사용한다
- `*ApplicationTest` 네이밍 사용 (예: `OrderApplicationServiceTest`, `DeductInventoryApplicationTest`)

**왜 이 레이어가 필요한가**:
EventProcessor는 ApplicationService를 mock해서 테스트한다.
따라서 ApplicationService 자체의 상태 전이 로직은 누군가 검증해야 한다.
이 테스트가 없으면 `order.confirm()` → `CONFIRMED` 같은 핵심 계약이 자동화된 테스트로 보호받지 못한다.

---

### 단위 테스트 — EventProcessor

**검증 대상**: 이벤트 수신 → ApplicationService 호출 → 발행 포트 호출 흐름의 조합

- ApplicationService와 EventPublisher를 모두 mock한다
- 어떤 이벤트가 들어왔을 때 어떤 서비스 메서드가 호출되고 어떤 이벤트가 발행되는지를 검증한다
- `*EventProcessorTest` 네이밍 사용

**왜 이 레이어가 필요한가**:
EventProcessor는 이벤트 수신과 서비스 호출, 이벤트 발행을 연결하는 조합 로직이다.
ApplicationService가 올바르게 동작하더라도 이 연결 고리가 잘못되면 Saga 흐름이 깨진다.

---

### 단위 테스트 — EventPublisher

**검증 대상**: `KafkaTemplate.send()`가 올바른 토픽과 페이로드로 호출되는지

- `KafkaTemplate`을 mock한다
- `*EventPublisherTest` 네이밍 사용

**왜 이 레이어가 필요한가**:
토픽 상수 오타나 페이로드 매핑 실수를 빠르게 잡을 수 있다.
Kafka 연결 없이 발행 계약을 문서화하는 역할도 한다.

---

### 통합 테스트 — HTTP/영속성

**검증 대상**: HTTP 요청 → 컨트롤러 → 서비스 → DB 저장까지의 전체 경로

- `@SpringBootTest` + H2 인메모리 DB 사용
- `*IntegrationTest` 네이밍 사용

**왜 order-service에만 있는가**:
`order-service`는 Saga의 유일한 HTTP 진입점이다.
`payment-service`와 `inventory-service`는 순수하게 Kafka 이벤트로만 구동된다.
HTTP 진입점이 없는 서비스에 HTTP/영속성 통합 테스트를 추가하는 것은 불필요하다.

---

### 시나리오 테스트 — End-to-End

**검증 대상**: 세 서비스가 실제 Kafka를 통해 Saga 흐름을 완주하는지

- Testcontainers로 실제 Kafka 컨테이너를 띄운다
- 세 서비스를 같은 JVM에서 `SpringApplicationBuilder`로 구동한다
- Awaitility로 비동기 상태 수렴을 검증한다
- `*ScenarioTest` 네이밍 사용

**왜 `scenario-test` 모듈을 분리했는가**:
세 서비스 모두에 의존하므로 어느 한 서비스 모듈에 두기 어렵다.
종단 간 흐름 검증은 단위/통합 테스트와 성격이 달라 분리하는 것이 명확하다.

---

## 현재 커버리지 매트릭스

| 레이어 | order-service | payment-service | inventory-service |
|---|---|---|---|
| ApplicationService 단위 | ✅ `OrderApplicationServiceTest` | ✅ `ProcessPaymentApplicationTest` | ✅ `DeductInventoryApplicationTest` |
| EventProcessor 단위 | ✅ `OrderEventProcessorTest` | ✅ `PaymentEventProcessorTest` | ✅ `InventoryEventProcessorTest` |
| EventPublisher 단위 | ✅ `OrderEventPublisherTest` | ✅ `PaymentEventPublisherTest` | ✅ `InventoryEventPublisherTest` |
| HTTP/영속성 통합 | ✅ `CreateOrderIntegrationTest` | 해당 없음 | 해당 없음 |
| 시나리오 (E2E) | ✅ `KafkaOrderProcessingScenarioTest` | — | — |
| 보상 시나리오 (E2E) | ✅ `KafkaCompensationScenarioTest` | — | — |

---

## 새 서비스 추가 시 기준

새 서비스를 추가할 때 아래 조건을 기준으로 테스트 구성을 결정한다.

- ApplicationService 단위 테스트: **항상 필요**
- EventProcessor 단위 테스트: Kafka 이벤트를 소비하는 서비스라면 **필요**
- EventPublisher 단위 테스트: Kafka 이벤트를 발행하는 서비스라면 **필요**
- HTTP/영속성 통합 테스트: HTTP 진입점이 있는 서비스라면 **필요**
- 시나리오 테스트: 새 Saga 흐름이 추가된다면 `scenario-test`에 **추가**
