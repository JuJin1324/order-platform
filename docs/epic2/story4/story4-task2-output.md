# Story 4 Task 2 산출물 — Hook / Page / Router 뼈대

---

## 완료한 작업

### React Router 설치

```bash
npm install react-router-dom
```

### `order-web/src/hooks/useOrderCreate.ts`

```ts
export function useOrderCreate() {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<OrderResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submitOrder(request: OrderRequest) { ... }

  return { submitOrder, isLoading, result, error };
}
```

Spring Application Service에 대응하는 계층이다. Page 컴포넌트로부터 이벤트를 받아 `api/orderApi.ts`를 호출하고, 상태(`isLoading`, `result`, `error`)를 관리한다. Page는 이 Hook이 돌려주는 값과 함수만 사용한다.

### `order-web/src/hooks/useOrderStatus.ts`

`useOrderCreate`와 동일한 구조. `fetchOrder(orderId)`를 호출하면 `getOrder` API 함수를 통해 주문을 조회한다. 상세 구현은 Story 6에서 완성한다.

### `order-web/src/pages/OrderCreatePage.tsx`

```tsx
export default function OrderCreatePage() {
  const { submitOrder, isLoading, result, error } = useOrderCreate();
  // 렌더링만 담당, api/ 직접 호출 없음
}
```

### `order-web/src/pages/OrderStatusPage.tsx`

```tsx
export default function OrderStatusPage() {
  const { fetchOrder, isLoading, result, error } = useOrderStatus();
  // 렌더링만 담당, api/ 직접 호출 없음
}
```

### `order-web/src/App.tsx` — React Router 구성 + 임시 fetch 코드 제거

```tsx
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/orders/new" element={<OrderCreatePage />} />
        <Route path="/orders/:id" element={<OrderStatusPage />} />
      </Routes>
    </BrowserRouter>
  );
}
```

Story 3 Task 2에서 추가한 임시 fetch 코드(`proxyStatus`, `useEffect`)를 제거했다. `BrowserRouter`가 앱 전체를 감싸고, `Routes` 안에서 URL과 컴포넌트를 매핑한다.

---

## 3계층 구조 완성

```
사용자 이벤트
    ↓
Page (OrderCreatePage, OrderStatusPage)   — 렌더링만
    ↓
Hook (useOrderCreate, useOrderStatus)     — 상태 관리 + 조율
    ↓
api/ (orderApi.ts)                        — HTTP 호출
    ↓
order-service (:8081)
```

---

## 테스트 게이트 확인

```bash
cd order-web && npm run dev
```

| 확인 항목 | 결과 |
|---|---|
| `tsc --noEmit` | 통과 |
| `http://localhost:5173/orders/new` | `OrderCreatePage` 렌더링 |
| `http://localhost:5173/orders/123` | `OrderStatusPage` 렌더링 |
| 뒤로가기·앞으로가기 | SPA 내에서 동작 |

---

## 머지 조건 확인

- [x] `hooks/useOrderCreate.ts`, `hooks/useOrderStatus.ts` 뼈대 생성
- [x] `pages/OrderCreatePage.tsx`, `pages/OrderStatusPage.tsx` 생성 — Hook만 호출, api/ 직접 호출 없음
- [x] React Router v6 설치 + 두 경로 라우팅 구성
- [x] `tsc --noEmit` 통과
- [x] 두 경로 접속 시 각 페이지 렌더링 확인
- [x] 뒤로가기·앞으로가기 동작 확인

---

## 다음 단계

Story 5 — `OrderCreatePage`에 주문 생성 폼 UI 구현 + `useOrderCreate` Hook 완성 + `POST /api/orders` 실제 호출
