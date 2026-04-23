# Story 7 Task 1 산출물 — 생성 → 조회 라우팅 연결 + URL 기반 자동 조회

---

## 완료한 작업

### `order-web/src/App.tsx` — 라우터 경로 정비

```tsx
<Route path="/orders/:orderId/status" element={<OrderStatusPage />} />
```

기존 `/orders/:id` 경로를 `/orders/:orderId/status`로 변경했다. URL 파라미터 이름을 `id` → `orderId`로 명확히 하고, `/status` 세그먼트를 추가해 "이 경로는 상태 조회 페이지"임을 URL만으로 알 수 있게 했다.

---

### `order-web/src/pages/OrderCreatePage.tsx` — 성공 시 자동 이동

```tsx
const navigate = useNavigate();

useEffect(() => {
  if (result) {
    navigate(`/orders/${result.orderId}/status`);
  }
}, [result]);
```

`result`가 set되는 순간 `useEffect`가 실행되어 상태 조회 페이지로 이동한다. `useNavigate`를 컴포넌트에서 사용하고, Hook은 navigation 책임을 알지 않는다.

---

### `order-web/src/pages/OrderStatusPage.tsx` — URL 기반 자동 조회

```tsx
const { orderId: urlOrderId } = useParams<{ orderId: string }>();

useEffect(() => {
  if (urlOrderId) fetchHistory(urlOrderId);
}, [urlOrderId]);
```

`useParams`로 URL의 `orderId`를 읽어 마운트 시점에 자동으로 이력을 조회한다. 수동 조회 폼도 그대로 유지되어 직접 주문 ID를 입력해 조회하는 경로도 동작한다. `urlOrderId`가 바뀔 때만 재실행되므로 불필요한 중복 조회가 없다.

---

## 테스트 게이트 확인

| 확인 항목 | 결과 |
|---|---|
| 폼 제출 → URL `/orders/{id}/status`로 자동 이동 | 확인 |
| 이동된 페이지에서 CREATED 이력 자동 표시 | 확인 |
| DevTools Network — POST → GET 순서 | 확인 |

---

## 머지 조건 확인

- [x] 주문 생성 성공 시 `/orders/{orderId}/status`로 자동 이동
- [x] 이동된 페이지에서 URL의 `orderId`로 이력 자동 조회
- [x] 수동 조회 폼도 여전히 동작
- [x] `tsc --noEmit` 통과

---

## 다음 단계

Task 2 — 뒤로가기·직접 URL 입력·폴백 라우트 등 SPA 예외 동작 보완 + 시연 시나리오 검증
