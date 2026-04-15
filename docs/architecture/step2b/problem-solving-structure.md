# 문제 해결 흐름 다이어그램 — Step 2b (보상 플로우)

이 문서는 Step 2a에서 해결하지 못했던 "중간 단계 실패 시 상태가 멈추는 문제"를, Kafka 보상 이벤트를 통해 어떻게 **최종 일관성(Eventual Consistency)** 상태로 수렴시키는지 보여준다.

이 문서가 답하려는 질문은 아래와 같다.

1. 어느 지점에서 실패하더라도 모든 서비스의 상태가 어떻게 '취소' 방향으로 정렬되는가?
2. Step 1, 2a와 비교했을 때 '일관성'의 관점에서 무엇이 진보했는가?
3. '비즈니스 로직' 차원의 보상과 '메시징 신뢰성' 차원의 문제는 어떻게 구분되는가?

---

## 1. 등장인물 및 상태 정의

Step 2b에서는 각 서비스가 **취소(CANCELLED)** 상태를 처리할 수 있는 로직이 추가된다.

| 참여자 | 역할 | 주요 상태 전이 |
|---|---|---|
| `order-service` | 주문 시작 및 최종 확정/취소 | `CREATED` → `CONFIRMED` 또는 `CANCELLED` |
| `payment-service` | 결제 승인 및 취소(환불) | `COMPLETED` → `CANCELLED` |
| `inventory-service` | 재고 차감 및 복구 | 차감 성공 또는 실패(이벤트 발행) |
| Kafka | 순방향/역방향 메시지 중개 | `order-created`, `payment-completed`, `inventory-failed` 등 |

---

## 2. 실패 시나리오 A — 결제 실패 (1단계 보상)

결제 서비스 자체에서 실패(예: 잔액 부족)가 발생했을 때의 흐름이다. 재고 단계까지 가기 전에 즉시 되돌린다.

```mermaid
sequenceDiagram
    autonumber
    participant O as order-service
    participant OD as order_db
    participant K1 as order-created 토픽
    participant P as payment-service
    participant PD as payment_db
    participant K2 as payment-failed 토픽

    O->>OD: 주문 CREATED 저장
    O->>K1: OrderCreatedEvent 발행
    
    K1->>P: consume
    P->>PD: 결제 실패 기록 (또는 기록 안 함)
    P->>K2: PaymentFailedEvent 발행
    
    K2->>O: consume
    O->>OD: 주문 CANCELLED 업데이트
```

---

## 3. 실패 시나리오 B — 재고 실패 (전체 보상 체인)

가장 복잡한 케이스로, 마지막 단계인 재고 차감이 실패했을 때 앞선 모든 단계를 역순으로 되돌린다.

```mermaid
sequenceDiagram
    autonumber
    participant O as order-service
    participant OD as order_db
    participant P as payment-service
    participant PD as payment_db
    participant K_PF as payment-cancelled 토픽
    participant I as inventory-service
    participant K_IF as inventory-failed 토픽

    Note over I: 재고 부족 발생
    I->>K_IF: InventoryDeductionFailedEvent 발행

    K_IF->>P: consume
    P->>PD: 결제 CANCELLED (환불 처리)
    P->>K_PF: PaymentCancelledEvent 발행

    K_PF->>O: consume
    O->>OD: 주문 CANCELLED 업데이트

    Note over OD,PD: 최종 상태: Order=CANCELLED, Payment=CANCELLED 로 수렴
```

---

## 4. 단계별 일관성 진화 비교

| 관점 | Step 1 (REST) | Step 2a (비동기 순방향) | Step 2b (Choreography Saga) |
|------|----------------|------------------------|---------------------------|
| **실패 시 현상** | 런타임 예외로 중단 | 이벤트 체인이 끊기고 방치됨 | 보상 이벤트로 역방향 전이 |
| **상태 불일치** | 영구적 (수동 복구 필요) | 영구적 (중간 상태에 머뒴) | **최종적으로 일관됨** |
| **가용성** | 낮음 (동기 체인) | 높음 (비동기) | 높음 (비동기 + 자동 복구) |
| **데이터 정합성** | 원자적이지 않음 | 원자적이지 않음 | **결과적 정합성 보장** |

---

## 5. 아직 해결하지 못한 문제 (Step 3의 영역)

Step 2b를 통해 "비즈니스 로직상의 보상"은 완성되었으나, **인프라/메시징 계층의 신뢰성** 문제는 남아있다.

1. **원자적 발행 (Atomic Write & Publish)**
   - 서비스가 DB 상태는 바꿨는데, Kafka에 이벤트를 던지기 직전에 죽는다면? (Outbox 패턴 필요)
2. **멱등성 (Idempotency)**
   - 보상 이벤트가 중복으로 소비되어 이미 취소된 결제를 또 취소하려고 한다면?
3. **이벤트 순서 보장**
   - 네트워크 지연으로 인해 `payment-cancelled`가 `payment-completed`보다 먼저 도착한다면?

Step 2b는 **"정상적인 메시지 전달 상황에서의 비즈니스 복구"**를 해결한 것이며, 위와 같은 "비정상 메시징 상황"은 Step 3에서 다룬다.

---

## 6. 검증 체크리스트 (통합 테스트 시나리오)

- [ ] **성공 케이스**: 모든 서비스 DB가 최종 확정 상태인가? (`CONFIRMED`, `COMPLETED`)
- [ ] **결제 실패 케이스**: 주문 서비스 DB가 결국 `CANCELLED`로 변하는가?
- [ ] **재고 실패 케이스**:
    - [ ] 결제 서비스 DB가 `COMPLETED`에서 `CANCELLED`로 변하는가?
    - [ ] 주문 서비스 DB가 `CANCELLED`로 변하는가?
- [ ] **지연 시뮬레이션**: 보상 흐름이 진행되는 중간에 로그를 확인했을 때, 상태가 순차적으로 되돌려지는가?
