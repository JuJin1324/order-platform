# Story 5 Task 1 산출물 — 주문 생성 폼 + Happy Path

---

## 완료한 작업

### `order-web/src/pages/OrderCreatePage.tsx` — 폼 UI 구현

```tsx
export default function OrderCreatePage() {
  const { submitOrder, isLoading, result, error } = useOrderCreate();

  const [sku, setSku] = useState('');
  const [quantity, setQuantity] = useState('');
  const [amount, setAmount] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    submitOrder({
      sku,
      quantity: Number(quantity),
      amount: Number(amount),
    });
  }

  return (
    <form onSubmit={handleSubmit}>
      <input value={sku} onChange={(e) => setSku(e.target.value)} required />
      <input type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
      <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} required />
      <button type="submit" disabled={isLoading}>
        {isLoading ? '처리 중...' : '주문 생성'}
      </button>
      {result && <p>주문 ID: {result.orderId} / 상태: {result.status}</p>}
      {error && <p>오류: {error}</p>}
    </form>
  );
}
```

`useOrderCreate` Hook은 Story 4에서 이미 완성된 상태였다. 이 Task에서는 Page에 폼 UI를 추가하는 것만으로 API 호출까지 연결됐다. 3계층 분리가 의도대로 동작한 것이다.

---

## 오류 발생 및 해결 과정

### 오류 1 — `React.FormEvent` import 누락

**증상**: 빈 흰색 화면만 표시됨.

**원인**: `handleSubmit` 함수의 파라미터 타입을 `React.FormEvent`로 선언했는데, 파일 상단에 `React` import가 없었다.

```tsx
// 잘못된 코드
import { useState } from 'react';
function handleSubmit(e: React.FormEvent) { ... }  // React를 import하지 않아서 오류
```

**해결**: `FormEvent`를 `react`에서 직접 named import로 가져오도록 수정했다.

```tsx
import { useState, FormEvent } from 'react';
function handleSubmit(e: FormEvent) { ... }
```

---

### 오류 2 — `verbatimModuleSyntax`로 인한 타입 import 오류

**증상**: 브라우저 콘솔에 아래 오류가 발생하며 흰색 화면이 표시됨.

```
Uncaught SyntaxError: The requested module '/src/types/order.ts'
does not provide an export named 'OrderRequest' (at orderApi.ts:1:10)
```

**원인**: TypeScript의 `interface`는 타입 검사용으로만 존재하고, 브라우저에 전달되는 JavaScript로 변환될 때 완전히 제거된다. 즉 `order.ts`에 `interface OrderRequest`가 선언되어 있어도, 런타임에서 이 파일을 불러오면 `interface OrderRequest`는 존재하지 않는다.

그런데 `orderApi.ts`에서 `import { OrderRequest } from '../types/order'`처럼 일반 import를 사용하면, Vite는 이것이 타입인지 값인지 판단하지 못하고 런타임에서 실제로 파일을 불러와 `OrderRequest`를 찾으려 한다. 막상 찾으면 이미 제거된 뒤라 오류가 발생한다.

`tsc --noEmit`은 통과했지만 Vite에서 런타임 오류가 난 이유도 여기 있다. TypeScript 컴파일러는 `OrderRequest`가 타입임을 알고 있어서 문제없이 처리하지만, Vite는 그 맥락 없이 구문만 보고 판단하기 때문이다.

**해결**: `import type`을 명시해서 Vite에게 "이건 타입이니까 JS로 변환할 때 이 줄을 아예 제거해도 된다"를 알렸다. `import type`으로 선언된 줄은 브라우저에 전달되는 JS에 포함되지 않는다.

```ts
// 수정 전
import { OrderRequest, OrderResponse } from '../types/order';

// 수정 후
import type { OrderRequest, OrderResponse } from '../types/order';
```

적용 파일: `api/orderApi.ts`, `hooks/useOrderCreate.ts`, `hooks/useOrderStatus.ts`

---

## 테스트 게이트 확인

`http://localhost:5174/orders/new` 접속 후 아래 값으로 폼 제출:

- 상품 ID: `ITEM-001`
- 수량: `2`
- 결제 금액: `29900`

| 확인 항목 | 결과 |
|---|---|
| DevTools Network 탭 `POST /api/orders` | 확인 |
| 응답 `orderId` 화면 표시 | 확인 |
| 응답 `status: CREATED` 화면 표시 | 확인 |

---

## 머지 조건 확인

- [x] 주문 생성 폼 UI 구현 (sku, quantity, amount 필드)
- [x] 폼 제출 시 `POST /api/orders` 실제 호출
- [x] 응답 `orderId`, `status` 화면 표시
- [x] 로딩 중 버튼 `disabled` 처리
- [x] `tsc --noEmit` 통과

---

## 다음 단계

Task 2 — 에러 처리 + UX 개선 (에러 메시지 표시, 재제출 시 초기화, 성공 후 폼 필드 초기화)
