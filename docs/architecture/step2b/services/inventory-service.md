# Inventory Service 상세 설계 — Step 2b

## 1. 역할 및 책임
- `PaymentCompletedEvent` 수신 시 재고 차감 처리
- 차감 성공 시 `InventoryDeductedEvent` 발행
- **(추가)** 재고 부족 등 실패 시 `InventoryDeductionFailedEvent` 발행 (보상 트리거)

## 2. 데이터 모델 (Product/Stock)
| 필드 | 타입 | 설명 |
|---|---|---|
| `productId` | Long | PK |
| `stockQuantity` | Integer | 현재 재고량 |

## 3. 메시징 명세

### 발행 (Publish)
- **Topic**: `inventory-deducted`
  - **Payload**: `orderId`, `productId`, `quantity`
- **Topic**: `inventory-deduction-failed` (Saga 보상 트리거)
  - **Payload**: `orderId`, `productId`, `reason`

### 구독 (Subscribe)
- **Topic**: `payment-completed`
  - **Action**: 재고 차감 시도. 성공 시 `inventory-deducted` 발행, 실패 시 `inventory-deduction-failed` 발행

## 4. 보상 로직 (Compensation)
- `inventory-service`는 가장 마지막 단계에서 **"실패를 선언"**하는 책임을 진다. 
- 여기서 발행된 실패 이벤트는 역순으로 `payment-service`와 `order-service`에 전달되어 각 서비스의 일관성을 맞추는 기폭제가 된다.
