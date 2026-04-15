# Step 2b 보상 트랜잭션 PR 계획

이 문서는 "Step 2a에서 완성된 순방향 Kafka 흐름 위에, 실패 시 각 서비스 상태를 되돌리는 보상 체인을 어떻게 단계적으로 쌓을 것인가"를 정리한다.

---

## 1. 왜 단계적으로 쌓아야 하는가

Step 2b의 보상 흐름은 역방향 의존성을 가진다.

```
inventory-service → payment-service → order-service
   (실패 기점)        (보상 실행자)      (최종 수렴자)
```

이 구조에서 각 PR은 앞 PR이 완성되어야 독립적으로 검증할 수 있다.

- `inventory-service`가 `InventoryDeductionFailedEvent`를 발행하지 않으면, `payment-service`의 구독 로직을 테스트할 기반이 없다.
- `payment-service`가 `PaymentCancelledEvent`를 발행하지 않으면, `order-service`의 최종 취소 로직을 검증할 방법이 없다.

따라서 Step 2b는 **실패의 기점(inventory)에서 시작해 최종 수렴자(order) 방향으로 순서대로 쌓는 방식**이 안전하다.

한 번에 세 서비스의 보상 로직을 동시에 작성하면, 어느 시점에도 "이 PR만으로 동작하는 것을 확인했다"는 명확한 기준점을 잡기 어렵다.

---

## 2. 구현 원칙

- **의미적 롤백(Semantic Rollback)**: 이미 기록된 결제나 주문을 삭제하지 않는다. 상태를 `CANCELLED`로 전이시켜 이력을 남긴다. 이벤트 소싱 관점에서 "무언가가 일어났다는 사실"은 지울 수 없다.
- **보상 책임 경계**: 각 서비스는 자신이 발행한 성공 이벤트에 대응하는 실패/취소 이벤트를 수신했을 때만 보상 로직을 실행한다. 다른 서비스의 DB를 직접 건드리지 않는다.
- **Step 2b 범위 고정**: 이 단계는 "정상적인 메시지 전달 상황에서의 비즈니스 보상"만 완성한다. 메시지 유실, 중복 소비, 타임아웃 같은 메시징 신뢰성 문제는 Step 3의 몫으로 남긴다.

---

## 3. 고정 결정

이 문서를 작성하는 시점에서 이미 확정된 설계 결정들이다. PR 작성 중 흔들리지 않도록 여기에 명시한다.

| 항목 | 결정 |
|---|---|
| 보상 방향 | 역방향 체인 (Choreography). Orchestrator를 두지 않는다. |
| 롤백 방식 | 레코드 삭제 없이 `CANCELLED` 상태 전이 |
| 이벤트 발행 실패 시 처리 | Step 2b에서는 다루지 않는다 (Step 3 — Outbox 패턴) |
| 중복 소비 처리 | Step 2b에서는 다루지 않는다 (Step 3 — 멱등성) |
| 신규 토픽 생성 방식 | 로컬은 `AUTO_CREATE_TOPICS_ENABLE: true`로 자동 생성 |
| 시나리오 테스트 방식 | Step 2a와 동일하게 Testcontainers 기반으로 추가 |

---

## 4. PR 순서

### PR 1. `saga-events` — 보상 이벤트 및 토픽 상수 추가

#### 의도

보상 체인에 필요한 이벤트 계약을 먼저 확정한다.
이후 PR들이 같은 타입을 참조할 수 있도록 공유 모듈에 먼저 올려두는 것이다.
서비스 로직을 한 줄도 바꾸기 전에 "무엇을 주고받을 것인가"를 코드로 고정하면, 이후 PR의 리뷰 기준이 명확해진다.

#### 핵심 작업

- `SagaTopics`에 보상 토픽 상수 추가
  - `INVENTORY_DEDUCTION_FAILED = "inventory-deduction-failed"`
  - `PAYMENT_FAILED = "payment-failed"`
  - `PAYMENT_CANCELLED = "payment-cancelled"`
- 보상 이벤트 레코드 추가
  - `InventoryDeductionFailedEvent(orderId, sku, reason)`
  - `PaymentFailedEvent(orderId, reason)`
  - `PaymentCancelledEvent(orderId, paymentId)`

#### 이 PR에서 하지 않을 것

- 각 서비스의 발행/구독 로직 변경
- 기존 이벤트 수정

#### 테스트 게이트

- 기존 전체 테스트 통과

#### 머지 조건

이 PR이 머지되면 이후 PR들이 공통 이벤트 타입을 참조할 수 있어야 한다.
이벤트 레코드의 필드 구성이 각 서비스가 실제로 필요한 정보를 담고 있는지 리뷰 시 확인한다.

---

### PR 2. `inventory-service` — 재고 차감 실패 시 보상 이벤트 발행

#### 의도

보상 체인의 출발점을 완성한다.
재고 차감이 실패했을 때 아무 이벤트도 없으면, 결제와 주문은 각자의 성공 상태에 영구적으로 머문다.
이 PR은 그 침묵을 깨고 보상 체인을 시작하는 신호를 만드는 작업이다.

현재 `InventoryEventProcessor.handlePaymentCompleted`는 성공 경로만 있다.
재고가 부족하면 예외가 던져질 뿐, 그 실패를 Kafka로 전파하는 로직이 없다.

#### 핵심 작업

- `InventoryEventProcessor.handlePaymentCompleted`에 실패 분기 추가
  - 재고 차감 실패 시 `InventoryDeductionFailedEvent` 발행
  - 성공 시 기존 `InventoryDeductedEvent` 발행 경로 유지
- `InventoryEventPublisher`에 `publishInventoryDeductionFailed` 추가
- `DeductInventoryResult` 또는 예외 처리 방식에 따라 실패/성공 분기 명확히 정의

#### 이 PR에서 하지 않을 것

- `payment-service`의 구독 로직 변경
- 재고 수량 데이터 모델 변경

#### 테스트 게이트

- `InventoryEventProcessor` 단위 테스트: 재고 부족 시 `InventoryDeductionFailedEvent` 발행 확인
- `InventoryEventProcessor` 단위 테스트: 재고 충분 시 기존 성공 경로 유지 확인
- `:inventory-service:test` 전체 통과
- 기존 시나리오 테스트 통과 (성공 경로 회귀 없음)

#### 머지 조건

재고 부족 시나리오에서 `InventoryDeductionFailedEvent`가 발행되는 것이 단위 테스트로 증명되어야 한다.
이 시점에는 아직 아무도 그 이벤트를 소비하지 않으므로, 전체 보상 체인이 동작하지 않는 것은 정상이다.

---

### PR 3. `payment-service` — 결제 실패 발행 + 재고 실패 수신 후 보상 처리

#### 의도

`payment-service`는 보상 체인에서 두 가지 역할을 동시에 맡는다.
첫째, 자신이 결제에 실패했을 때 이를 직접 알린다(`PaymentFailedEvent`).
둘째, 자신 뒤에서 재고가 실패했다는 신호를 받아 이미 완료된 결제를 되돌리고 다음 보상자에게 넘긴다(`PaymentCancelledEvent`).

이 두 역할을 같은 PR에서 다루는 이유는, 둘 다 payment-service 내부에서 완결되는 변경이고 서로 성격이 맞닿아 있기 때문이다.
결제 실패 발행(`PaymentFailed`)과 재고 실패 수신 후 취소 발행(`PaymentCancelled`)은 모두 "결제가 정상 완료 상태에서 벗어나는 경우"라는 같은 맥락에 속한다.

현재 `PaymentEventProcessor.handleOrderCreated`는 결제를 처리한 뒤 항상 `PaymentCompletedEvent`를 발행한다.
결제 자체가 실패하는 경우에 대한 분기가 없고, `inventory-deduction-failed` 토픽을 구독하는 로직도 없다.

#### 핵심 작업

- `PaymentEventProcessor.handleOrderCreated`에 실패 분기 추가
  - 결제 실패 시 `PaymentFailedEvent` 발행
  - 성공 시 기존 `PaymentCompletedEvent` 발행 경로 유지
- `inventory-deduction-failed` 구독 추가
  - `PaymentEventListener`에 `onInventoryDeductionFailed` 추가
  - `PaymentEventProcessor.handleInventoryDeductionFailed` 추가
    - 해당 주문의 결제 상태를 `CANCELLED`로 전이
    - `PaymentCancelledEvent` 발행
- `PaymentEventPublisher`에 `publishPaymentFailed`, `publishPaymentCancelled` 추가
- `Payment` 도메인 엔티티에 `CANCELLED` 상태 전이 로직 추가 (이미 있다면 확인)

#### 이 PR에서 하지 않을 것

- `order-service`의 구독 로직 변경

#### 테스트 게이트

- `PaymentEventProcessor` 단위 테스트: 결제 실패 시 `PaymentFailedEvent` 발행 확인
- `PaymentEventProcessor` 단위 테스트: `InventoryDeductionFailed` 수신 시 결제 `CANCELLED` 전이 + `PaymentCancelledEvent` 발행 확인
- `PaymentEventProcessor` 단위 테스트: 결제 성공 시 기존 성공 경로 유지 확인
- `:payment-service:test` 전체 통과
- 기존 시나리오 테스트 통과

#### 머지 조건

결제 실패 경로와 재고 실패 수신 후 보상 경로 모두 단위 테스트로 증명되어야 한다.
이 시점에는 `order-service`가 아직 보상 이벤트를 구독하지 않으므로, 전체 체인은 완성되지 않는다. 이는 정상이다.

---

### PR 4. `order-service` — 보상 이벤트 수신 후 주문 취소

#### 의도

보상 체인의 최종 수렴자를 완성한다.
`order-service`는 Saga의 시작점이자 종착점이다.
성공 경로에서는 `InventoryDeductedEvent`를 받아 주문을 `CONFIRMED`로 닫고,
실패 경로에서는 `PaymentFailed` 또는 `PaymentCancelled`를 받아 주문을 `CANCELLED`로 닫는다.

현재 `OrderEventListener`는 `inventory-deducted`만 구독하고 있다.
결제 실패나 결제 취소를 받아 주문을 되돌리는 로직이 없어, 보상 체인이 여기서 끊긴다.
이 PR이 끝나면 세 서비스가 실패 시 모두 `CANCELLED` 상태로 수렴하는 전체 보상 체인이 처음으로 완성된다.

#### 핵심 작업

- `payment-failed`, `payment-cancelled` 토픽 구독 추가
  - `OrderEventListener`에 `onPaymentFailed`, `onPaymentCancelled` 추가
  - `OrderEventProcessor`에 `handlePaymentFailed`, `handlePaymentCancelled` 추가
    - 해당 주문 상태를 `CANCELLED`로 전이
- `OrderApplicationService`에 `cancelOrder` 추가 (또는 기존 메서드 재사용 가능 여부 확인)

#### 이 PR에서 하지 않을 것

- 보상 시나리오 테스트 추가 (다음 PR에서 별도 추가)
- 기존 성공 경로 변경

#### 테스트 게이트

- `OrderEventProcessor` 단위 테스트: `PaymentFailed` 수신 시 주문 `CANCELLED` 전이 확인
- `OrderEventProcessor` 단위 테스트: `PaymentCancelled` 수신 시 주문 `CANCELLED` 전이 확인
- `OrderEventProcessor` 단위 테스트: 기존 `InventoryDeducted` 수신 시 성공 경로 유지 확인
- `:order-service:test` 전체 통과
- 기존 시나리오 테스트 통과

#### 머지 조건

세 서비스의 보상 로직이 코드 수준에서 모두 연결된 상태여야 한다.
단, 전체 체인이 실제로 end-to-end로 동작하는 것은 다음 PR의 시나리오 테스트에서 최초로 증명한다.

---

### PR 5. 보상 흐름 시나리오 테스트 추가

#### 의도

PR 2~4에 걸쳐 작성된 보상 로직이 실제로 end-to-end로 동작하는지를 처음으로 증명한다.
각 PR의 단위 테스트는 개별 컴포넌트의 행동을 검증했지만, 세 서비스가 함께 실제 Kafka를 통해 보상 체인이 완주하는지는 아직 아무도 본 적이 없다.
이 PR이 끝나야 "Step 2b가 완성됐다"고 말할 수 있다.

기존 성공 경로 시나리오 테스트(`KafkaOrderProcessingScenarioTest`)는 건드리지 않는다.
보상 흐름은 완전히 다른 검증 목적이므로 별도 테스트 클래스로 분리한다.

#### 핵심 작업

- `KafkaCompensationScenarioTest` 추가

  **시나리오 A — 결제 실패 (1단계 보상)**
  - 결제가 항상 실패하는 조건으로 주문 생성
  - 최종적으로 주문 `CANCELLED` 수렴 확인

  **시나리오 B — 재고 실패 (전체 보상 체인)**
  - 재고가 0인 상태에서 주문 생성
  - 최종적으로 결제 `CANCELLED` + 주문 `CANCELLED` 수렴 확인

- 결제 실패 조건 주입 방법 결정
  - 예: 특정 금액 또는 특정 SKU를 항상 실패로 처리하는 테스트용 분기 추가

#### 이 PR에서 하지 않을 것

- 기존 성공 경로 시나리오 테스트 수정
- 멱등성, Outbox, DLT 관련 검증 (Step 3)

#### 테스트 게이트

- 기존 성공 경로 시나리오 테스트 통과 (회귀 없음)
- 시나리오 A (결제 실패) 통과
- 시나리오 B (재고 실패, 전체 롤백) 통과

#### 머지 조건

세 서비스가 Testcontainers Kafka 위에서 함께 돌아가며, 실패 케이스에서 모든 서비스가 `CANCELLED`로 수렴하는 것이 자동화된 테스트로 증명되어야 한다.
이 PR이 머지되면 Step 2b가 완성된 것으로 본다.

---

### PR 6. 테스트 전략 체계화 및 누락 단위 테스트 보완

#### 의도

PR 1~5가 Step 2b의 기능을 완성하지만, 단위 테스트의 레이어별 커버리지가 서비스마다 불균형하다.
이 PR은 기능 변경 없이 테스트 품질을 고르게 만드는 것이 목적이다.

현재 불균형:

| 레이어 | order | payment | inventory |
|---|---|---|---|
| EventProcessor 단위 | ✅ | ✅ | ✅ |
| EventPublisher 단위 | ✅ | ✅ | ✅ |
| ApplicationService 단위 | ❌ | ✅ | ✅ |
| HTTP/영속성 통합 | ✅ | ❌ | ❌ |

HTTP/영속성 통합 테스트가 order에만 있는 것은 의도적이다.
order-service는 Saga의 유일한 HTTP 진입점이고, payment·inventory는 순수하게 이벤트로만 구동된다.
반면 `OrderApplicationService`의 단위 테스트 누락은 의도한 게 아니라 단계적 개발 과정에서 생긴 공백이다.

#### 핵심 작업

- `OrderApplicationServiceTest` 추가
  - `confirmOrder` 성공 케이스
  - `cancelOrder` 성공 케이스
  - `failOrder` 성공 케이스
  - 존재하지 않는 orderId 조회 시 예외 케이스
- `docs/` 아래 테스트 전략 문서 추가
  - 서비스별·레이어별 테스트 매트릭스
  - 각 레이어에 테스트가 필요한 이유 명시
  - 앞으로 새 서비스나 레이어 추가 시 기준으로 사용

#### 이 PR에서 하지 않을 것

- 기능 코드 변경
- payment·inventory HTTP 통합 테스트 추가 (진입점이 없으므로 불필요)

#### 테스트 게이트

- 기존 전체 테스트 통과 (회귀 없음)
- 새로 추가된 `OrderApplicationServiceTest` 통과

---

## 5. 완료 판단 기준

아래 조건이 모두 충족되기 전까지는 Step 2b가 완성된 것으로 보지 않는다.

- 재고 실패 시 결제와 주문이 자동으로 `CANCELLED`로 수렴하는 시나리오 테스트가 통과한다
- 결제 실패 시 주문이 자동으로 `CANCELLED`로 수렴하는 시나리오 테스트가 통과한다
- 기존 성공 경로 시나리오 테스트가 여전히 통과한다 (보상 로직 추가가 성공 경로를 건드리지 않았음을 보장)
- 각 서비스의 보상 관련 단위 테스트가 모두 존재한다

---

## 6. Step 3으로 의도적으로 미루는 것

Step 2b는 "정상적인 메시지 전달 상황에서의 비즈니스 보상"만 완성한다.
아래 항목들은 이번 단계의 머지 조건에 넣지 않는다.

| 항목 | 이유 |
|---|---|
| **Outbox 패턴** | DB 저장과 Kafka 발행의 원자성 보장. 서비스가 저장 후 발행 직전에 죽는 경우를 다룬다. |
| **멱등성** | 보상 이벤트가 중복 소비되어 이미 취소된 결제를 또 취소하려는 상황을 방어한다. 수동 커밋 도입과 함께 다뤄야 의미가 있다. |
| **타임아웃 보상** | 재고 서비스가 응답도 없고 실패 이벤트도 발행하지 않고 죽은 경우, 결제와 주문이 영구적으로 중간 상태에 머무는 문제. Saga Orchestrator 또는 별도 Timeout 처리가 필요하다. |
| **DLT(Dead Letter Topic)** | retry를 소진한 메시지를 격리하는 장치. 기본 흐름을 막지 않으려면 retry 정책과 함께 설계해야 한다. |
| **Result 타입 (도메인 실패 모델링)** | 재고 부족 같은 예측 가능한 실패를 예외가 아닌 반환 타입으로 표현. 보상 이벤트 프로세서의 try/catch 제거. Vavr `Either` 또는 sealed interface 기반 `Result` 도입 검토. Step 2b에서 `InventoryEventProcessor`가 `IllegalStateException`을 catch해 이벤트로 번역하는 구조가 어색함의 근원. |

한 줄 요약: `Step 2b는 "무엇을 되돌릴 것인가"를 완성하고, "어떻게 안전하게 전달할 것인가"는 Step 3에서 다룬다.`

---

## 7. PR 실행 순서 요약

| 순서 | PR | 완성되는 것 |
|---|---|---|
| 1 | `saga-events` 보상 이벤트/토픽 추가 | 보상 체인의 공유 계약 확정 |
| 2 | `inventory-service` 실패 발행 | 보상 체인의 시작 신호 완성 |
| 3 | `payment-service` 실패 발행 + 보상 처리 | 보상 체인의 중간 연결 완성 |
| 4 | `order-service` 보상 수신 | 보상 체인의 종착점 완성 |
| 5 | 보상 시나리오 테스트 | 전체 보상 체인 end-to-end 증명 |
| 6 | 테스트 전략 체계화 및 누락 단위 테스트 보완 | 서비스 간 테스트 커버리지 균형 확보 |
