# Payment Service 상세 설계 — Step 2b

## 1. 역할 및 책임
- `OrderCreatedEvent` 수신 시 결제 처리 (`COMPLETED`)
- **(추가)** 결제 성공 시 `PaymentCompletedEvent` 발행
- **(추가)** 결제 자체 실패 시 `PaymentFailedEvent` 발행
- **(추가)** `InventoryDeductionFailed` 수신 시 결제 상태 취소 (`CANCELLED`) 및 보상 이벤트 발행

## 2. 데이터 모델 (Payment)
| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `orderId` | Long | FK |
| `amount` | Long | 결제 금액 |
| `status` | Enum | `COMPLETED`, `FAILED`, `CANCELLED` |

## 3. 메시징 명세

### 발행 (Publish)
- **Topic**: `payment-completed`
  - **Payload**: `orderId`, `amount`
- **Topic**: `payment-failed`
  - **Payload**: `orderId`, `reason`
- **Topic**: `payment-cancelled` (보상 전파용)
  - **Payload**: `orderId`, `paymentId`

### 구독 (Subscribe)
- **Topic**: `order-created`
  - **Action**: 결제 처리 후 `payment-completed` 또는 `payment-failed` 발행
- **Topic**: `inventory-deduction-failed`
  - **Action**: 해당 결제 상태를 `CANCELLED`로 변경하고 `payment-cancelled` 발행

## 4. 보상 로직 (Compensation)
- `payment-service`는 중간 단계로서, **"뒷 단계 실패 시 앞 단계에 이를 알리는 역할"**을 수행한다.
- 단순히 상태만 바꾸는 것이 아니라, 이력을 남기거나 환불 처리(시뮬레이션)를 수행하는 것이 핵심이다.
