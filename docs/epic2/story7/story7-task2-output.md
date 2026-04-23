# Story 7 Task 2 산출물 — SPA 예외 동작 보완 + 시연 시나리오 검증

---

## 완료한 작업

### `order-web/src/App.tsx` — 폴백 라우트 추가

```tsx
<Route path="*" element={<Navigate to="/orders/new" replace />} />
```

**폴백 라우트란**: 정의된 경로(`/orders/new`, `/orders/:orderId/status`) 중 어디에도 매칭되지 않는 모든 URL을 잡는 라우트다. `path="*"`가 와일드카드 역할을 한다. 폴백 라우트가 없으면 `/asdf` 같은 비정상 URL로 접근했을 때 빈 화면이 표시된다. `replace` 옵션을 주면 히스토리 스택에 리다이렉트 전 URL이 남지 않아 뒤로가기 시 무한 루프가 발생하지 않는다.

### `order-web/src/pages/OrderCreatePage.tsx` — 폼 기본값 수정

```tsx
const [sku, setSku] = useState('sku-001');
```

기존 `ITEM-001`에서 `sku-001`로 변경. `sku-001`이 inventory-service에 초기 재고 10개로 등록된 유일한 SKU다.

---

## 시연 시나리오

### CONFIRMED 시나리오

inventory-service `data.sql`에 `sku-001`이 재고 10개로 초기화되어 있다.

| 필드 | 값 |
|---|---|
| 상품 ID (SKU) | `sku-001` (폼 기본값) |
| 수량 | `2` |
| 결제 금액 | `29900` |

재고가 있으므로 inventory-service가 차감에 성공하고 Saga가 정상 완료된다. 잠시 후 새로고침하면 `CONFIRMED`가 추가된다.

### CANCELLED 시나리오

존재하지 않는 SKU를 사용하면 inventory-service가 보상 이벤트를 발행해 Saga가 취소된다.

| 필드 | 값 |
|---|---|
| 상품 ID (SKU) | `ITEM-001` (inventory DB에 없는 SKU) |
| 수량 | `2` |
| 결제 금액 | `29900` |

Saga 흐름: `order-created` → payment 완료 → inventory 차감 실패(`ITEM-001` 없음) → `inventory-deduction-failed` → payment 보상 → `payment-cancelled` → `CANCELLED`

---

## 시연 체크리스트

- [x] 폼 제출 → 자동 이동 → `CREATED` 표시
- [x] 잠시 후 새로고침 → `CONFIRMED` 추가 (SKU: `sku-001`, 수량: `2`)
- [x] 재고 없는 시나리오 → 새로고침 → `CANCELLED` 추가 (SKU: `ITEM-001`)
- [x] 상태 페이지에서 뒤로가기 → 주문 생성 페이지 복귀
- [x] 주소창에 `/orders/{유효 ID}/status` 직접 입력 → 정상 조회
- [x] 주소창에 `/orders/없는ID/status` 직접 입력 → "이력이 없습니다" 표시
- [x] 비정상 URL(`/asdf`) 진입 → `/orders/new`로 리다이렉트

---

## 오류 발생 및 해결 과정

### 오류 1 — Saga가 CONFIRMED/CANCELLED로 전이되지 않음

**증상**: 주문 생성 후 새로고침해도 `CREATED`에서 변하지 않는다.

**원인 1 — Kafka 리스너 설정 오류**: `docker-compose.yml`의 `KAFKA_ADVERTISED_LISTENERS`가 `PLAINTEXT://localhost:9092`로 설정되어 있었다. 컨테이너 내부에서 `localhost`는 자기 자신을 가리키므로 order-service가 Kafka 컨슈머 코디네이터에 연결하지 못해 이벤트를 수신할 수 없었다.

**해결**: 컨테이너 간 통신용 내부 리스너(`PLAINTEXT_INTERNAL://kafka:29092`)와 호스트 접근용 외부 리스너(`PLAINTEXT_EXTERNAL://localhost:9092`)를 분리했다.

```yaml
KAFKA_LISTENERS: PLAINTEXT_INTERNAL://:29092,PLAINTEXT_EXTERNAL://:9092,CONTROLLER://:9093
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT_INTERNAL://kafka:29092,PLAINTEXT_EXTERNAL://localhost:9092
KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
```

서비스들의 `KAFKA_BOOTSTRAP_SERVERS`도 `kafka:9092` → `kafka:29092`로 변경했다.

**원인 2 — inventory-service 예외 처리 누락**: `ITEM-001`처럼 존재하지 않는 SKU로 주문하면 `CANCELLED`가 떠야 하는데 나타나지 않았다. `InventoryEventProcessor`의 catch 블록이 `IllegalStateException`(재고 부족)만 잡고, SKU가 없을 때 발생하는 `IllegalArgumentException`은 잡지 않았다. 예외가 catch를 통과하지 못해 `inventory-deduction-failed` 보상 이벤트가 발행되지 않았다.

**해결**: catch 블록에 `IllegalArgumentException`을 추가했다.

```java
} catch (IllegalStateException | IllegalArgumentException e) {
    inventoryEventPublisher.publishInventoryDeductionFailed(...);
}
```

---

## 머지 조건 확인

- [x] 폴백 라우트 추가 — 비정상 URL → `/orders/new` 리다이렉트
- [x] `tsc --noEmit` 통과
- [x] 시연 체크리스트 전체 통과

---

## 다음 단계

Task 3 — Tailwind CSS 설치 + 두 페이지 UI 스타일 적용
