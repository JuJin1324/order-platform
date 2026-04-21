# Story 4 Task 1 산출물 — API / 타입 계층 정의

---

## 완료한 작업

### `order-web/src/types/order.ts` — TypeScript 타입 정의

백엔드 DTO와 1:1 매칭으로 작성했다.

```ts
export type OrderStatus = 'CREATED' | 'CONFIRMED' | 'CANCELLED';

export interface OrderRequest {
  sku: string;
  quantity: number;
  amount: number;
}

export interface OrderResponse {
  orderId: string;
  status: OrderStatus;
  sku: string;
  quantity: number;
  amount: number;
}
```

백엔드 DTO 대응:

| TypeScript | Java |
|---|---|
| `OrderRequest` | `CreateOrderRequest` (record) |
| `OrderResponse` | `OrderResult` (record) |
| `OrderStatus` | `OrderStatus` (enum) |
| `number` | `Integer`, `BigDecimal` |

`BigDecimal`은 TypeScript에 대응 타입이 없으므로 `number`로 받는다. JSON 직렬화 시 숫자로 변환되어 전달된다.

`OrderStatus`는 Java enum을 TypeScript union type으로 표현했다. enum 대신 union type을 쓰는 이유는 백엔드에서 문자열로 직렬화되어 오기 때문에 string 호환성이 자연스럽기 때문이다.

### `order-web/src/api/orderApi.ts` — HTTP 호출 함수

```ts
export async function createOrder(request: OrderRequest): Promise<OrderResponse> {
  const res = await fetch('/api/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!res.ok) throw new Error(`${res.status}`);
  return res.json();
}

export async function getOrder(orderId: string): Promise<OrderResponse> {
  const res = await fetch(`/api/orders/${orderId}`);
  if (!res.ok) throw new Error(`${res.status}`);
  return res.json();
}
```

`fetch`는 4xx/5xx를 에러로 간주하지 않으므로 `res.ok` 체크를 직접 한다. 이 계층은 상태 관리·UI 관심사를 모른다. 에러를 던지기만 하고, 처리는 Hook(Story 5)이 담당한다.

### 문서 수정

Tasks 문서와 Stories 문서의 `OrderStatus` enum 값 `PENDING` → `CREATED` 수정. 실제 백엔드 코드(`OrderStatus.java`) 기준으로 정정했다.

---

## 테스트 게이트 확인

```bash
cd order-web && npx tsc --noEmit
```

타입 오류 없음 확인.

---

## 머지 조건 확인

- [x] `order-web/src/types/order.ts` — 백엔드 DTO와 1:1 매칭 타입 정의
- [x] `order-web/src/api/orderApi.ts` — `createOrder`, `getOrder` 함수 구현
- [x] `tsc --noEmit` 통과
- [x] 앱 실행 시 기존 화면 정상 렌더링

---

## 다음 단계

Task 2 — `hooks/`, `pages/` 뼈대 + React Router 구성
