# Story 6 Task 2 산출물 — 프론트엔드: 상태 이력 타임라인 화면

---

## 완료한 작업

### `order-web/src/types/order.ts` — OrderStatusHistory 타입 추가

```ts
export interface OrderStatusHistory {
  status: OrderStatus;
  changedAt: string;
}
```

백엔드 `OrderStatusHistoryResult`와 1:1 매칭. `changedAt`은 `@JsonFormat`으로 ISO 8601 문자열로 직렬화되므로 `string`으로 받는다.

---

### `order-web/src/api/orderApi.ts` — getOrderStatusHistory 함수 추가

```ts
export async function getOrderStatusHistory(orderId: string): Promise<OrderStatusHistory[]> {
  const res = await fetch(`/api/orders/${orderId}/status-history`);
  if (!res.ok) throw new Error(`${res.status}`);
  return res.json();
}
```

---

### `order-web/src/hooks/useOrderStatus.ts` — 이력 조회 Hook 구현

```ts
export function useOrderStatus() {
  const [isLoading, setIsLoading] = useState(false);
  const [history, setHistory] = useState<OrderStatusHistory[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [lastOrderId, setLastOrderId] = useState<string | null>(null);

  async function fetchHistory(orderId: string) { ... }
  async function refresh() {
    if (lastOrderId) await fetchHistory(lastOrderId);
  }

  return { fetchHistory, refresh, isLoading, history, error };
}
```

`lastOrderId`를 Hook 내부에 보관해 새로고침 버튼 클릭 시 컴포넌트가 ID를 다시 넘기지 않아도 재조회가 동작한다.

---

### `order-web/src/pages/OrderStatusPage.tsx` — 타임라인 화면 구현

- 주문 ID 입력 필드 + 조회 버튼
- 이력 배열을 `status — changedAt` 형태 리스트로 시간순 렌더링
- 새로고침 버튼 — Hook의 `refresh()` 호출
- 이력이 없을 때 "이력이 없습니다" 메시지 표시
- 네트워크 오류 메시지 표시

---

## 오류 발생 및 해결 과정

### 이력 조회 불가 — Docker 이미지 미반영

**증상**: 주문 ID를 입력해도 이력이 표시되지 않음.

**원인**: Task 1에서 추가한 `GET /orders/{id}/status-history` 엔드포인트가 Docker 컨테이너에 반영되지 않은 상태였다. Docker 이미지가 Task 1 구현 전에 빌드된 JAR 기반이었기 때문이다.

**해결**: `make up`으로 JAR 재빌드 → 이미지 재빌드 → order-service 컨테이너 재시작.

```bash
make up
```

---

## 테스트 게이트 확인

| 시나리오 | 결과 |
|---|---|
| 주문 ID 입력 → 조회 → CREATED 이력 표시 | 확인 |
| 잠시 후 새로고침 버튼 → CONFIRMED 추가 | 확인 |
| 존재하지 않는 ID 입력 → "이력이 없습니다" | 확인 |
| 네트워크 Offline → 오류 메시지 표시 | 확인 |

---

## 머지 조건 확인

- [x] `OrderStatusHistory` 타입 + `getOrderStatusHistory` 함수 추가
- [x] `useOrderStatus` Hook — 이력 조회 + 새로고침 구현
- [x] `OrderStatusPage` — 주문 ID 입력 → 타임라인 렌더링 + 새로고침 버튼
- [x] 이력 없을 때 빈 상태 메시지 표시
- [x] 네트워크 오류 메시지 표시
- [x] `tsc --noEmit` 통과

---

## 다음 단계

Story 7 — 주문 생성 완료 후 상태 조회 페이지로 자동 이동 (React Router `navigate`)
