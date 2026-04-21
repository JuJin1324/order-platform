# 학습 — React Router

---

## React Router가 필요한 이유

SPA는 `index.html` 하나로 모든 화면을 처리한다. 브라우저 주소창의 URL이 바뀌어도 서버에 새 HTML을 요청하지 않는다. 문제는 `/orders/new`와 `/orders/123`이 같은 HTML을 받으면 어떤 컴포넌트를 보여줄지 알 수 없다는 점이다. React Router가 이 URL → 컴포넌트 매핑을 담당한다.

---

## 해주는 것

| 기능 | 설명 |
|---|---|
| **URL → 컴포넌트 매핑** | `/orders/new` 접속 시 `OrderCreatePage` 렌더링 |
| **URL 파라미터 추출** | `/orders/123`에서 `123`을 `useParams()`로 꺼낼 수 있음 |
| **페이지 이동** | `navigate('/orders/123')`으로 서버 요청 없이 화면 전환 |
| **뒤로가기·앞으로가기** | 브라우저 히스토리와 연동해서 SPA 내에서 동작 |

---

## order-web에서의 사용

```tsx
// App.tsx
<Routes>
  <Route path="/orders/new" element={<OrderCreatePage />} />
  <Route path="/orders/:id" element={<OrderStatusPage />} />
</Routes>
```

URL 파라미터 추출 (`OrderStatusPage` 내부):

```tsx
const { id } = useParams()  // /orders/123 접속 시 id === '123'
```

페이지 이동 (주문 생성 완료 후 상태 조회 페이지로):

```tsx
const navigate = useNavigate()
navigate(`/orders/${orderId}`)  // Story 7에서 사용
```

---

## React Router가 없으면

URL이 바뀌어도 화면이 바뀌지 않는다. 뒤로가기·앞으로가기도 동작하지 않는다. 직접 URL을 파싱하고 조건부 렌더링을 구현해야 하는데, 그게 React Router가 해주는 일이다.
