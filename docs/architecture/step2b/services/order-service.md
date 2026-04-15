# Order Service 상세 설계 — Step 2b

## 1. 역할 및 책임
- 주문 생성 요청 접수 (`CREATED` 상태 저장)
- `OrderCreatedEvent` 발행
- **(추가)** `InventoryDeductedEvent` 수신 시 주문 확정 (`CONFIRMED`)
- **(추가)** `PaymentCancelled` 또는 `PaymentFailed` 수신 시 주문 취소 (`CANCELLED`)

## 2. 데이터 모델 (Order)
| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `productId` | Long | 상품 ID |
| `quantity` | Integer | 주문 수량 |
| `status` | Enum | `CREATED`, `CONFIRMED`, `CANCELLED` |

## 3. 메시징 명세

### 발행 (Publish)
- **Topic**: `order-created`
  - **Payload**: `orderId`, `productId`, `quantity`, `amount`
- **Topic**: `order-cancelled` (최종 확정 실패 시 발행, 타 서비스 참고용)
  - **Payload**: `orderId`, `reason`

### 구독 (Subscribe)
- **Topic**: `inventory-deducted`
  - **Action**: 주문 상태를 `CONFIRMED`로 변경
- **Topic**: `payment-cancelled`
  - **Action**: 주문 상태를 `CANCELLED`로 변경
- **Topic**: `payment-failed`
  - **Action**: 주문 상태를 `CANCELLED`로 변경

## 4. 보상 로직 (Compensation)
- `order-service`는 Saga의 시작점이자 종착점이므로, 다른 서비스로부터 "최종 실패"를 통보받으면 자기에 기록된 주문을 `CANCELLED`로 업데이트하여 일관성을 맞춘다.
