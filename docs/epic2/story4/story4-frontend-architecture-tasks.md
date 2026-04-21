# Story 4 프론트엔드 아키텍처 세팅 Task 계획

이 문서는 "View / ViewModel / API 3계층 구조를 어떤 순서로 쌓을 것인가"를 정리한다.

---

## 1. 왜 두 Task로 나누는가

Story 4는 아키텍처 뼈대를 잡는 작업이다. 의존성 방향이 명확하다.

```
API(타입·HTTP 호출) ← ViewModel(Hook) ← View(Page·Component)
```

아래에서 위로 쌓아야 각 층이 다음 층의 기반으로 독립 검증된다.

- **Task 1**: 하위 계층(API + 타입)을 먼저 확정
- **Task 2**: 상위 계층(Hook + Page + Router)을 Task 1 위에 올림

반대 순서로 하면 "Hook이 `api/` 함수를 호출하는데 아직 타입이 없다" 같은 상황에서 타입을 임시로 채우게 되고, Task 1에서 다시 맞춰야 한다.

---

## 2. 고정 결정

- 폴더 구조: `pages/`, `components/`, `hooks/`, `api/`, `types/`
- API 클라이언트: `fetch` 사용 (axios 미도입 — 의존성 최소화)
- 상태 관리 라이브러리 미도입 — `useState` + React Router state로 충분
- 라우팅: React Router v6
- Custom Hook 명명: `use[Domain][Action]` (예: `useOrderCreate`, `useOrderStatus`)
- 타입 명명: 백엔드 DTO와 1:1 매칭 (예: `OrderRequest`, `OrderResponse`)

---

## 3. 권장 Task 순서

### Task 1. API / 타입 계층 정의

#### 목표

백엔드 REST API를 TypeScript 타입과 함수로 표현하여, 이후 모든 상위 계층이 이 계층을 통해 HTTP를 호출하는 구조를 만든다.

#### 핵심 작업

- `order-web/src/types/order.ts`: Swagger 문서 기반 DTO 타입 정의
  - `OrderRequest`, `OrderResponse`
  - `OrderStatus` enum (`CREATED`, `CONFIRMED`, `CANCELLED`)
- `order-web/src/api/orderApi.ts`: HTTP 호출 함수
  - `createOrder(request: OrderRequest): Promise<OrderResponse>`
  - `getOrder(orderId: string): Promise<OrderResponse>`
- 공통 fetch 래퍼(`order-web/src/api/client.ts` 등)는 호출 함수가 2개 이상 공통 로직을 실제로 공유하게 될 때만 추가한다

#### 설계 포인트

- `api/` 함수는 상태 관리·UI 관심사를 모른다
- 반환 타입을 `Promise<T>`로 단일화하고, HTTP status code 파싱은 이 계층이 책임진다
- Swagger에서 확인한 응답 스펙과 타입이 정확히 일치해야 한다

#### 이 Task에서 하지 않을 것

- Custom Hook (Task 2)
- React Router (Task 2)
- Page 컴포넌트 (Task 2)
- 에러 타입 정규화 — Story 5에서 실제 에러 처리와 함께

#### 테스트 게이트

- `tsc --noEmit` 통과
- 앱 실행 시 기존 화면 여전히 렌더링

#### 머지 조건

`api/orderApi.ts`의 함수를 Hook에서 import만 하면 쓸 수 있는 상태.

---

### Task 2. Hook / Page / Router 뼈대

#### 목표

Custom Hook과 페이지 컴포넌트의 빈 뼈대를 만들어, Story 5 구현이 이 위에 얹히는 형태가 되도록 한다.

#### 핵심 작업

- `order-web/src/hooks/useOrderCreate.ts`: 빈 Hook 뼈대
  - 반환 시그니처: `{ createOrder, isLoading, result, error }`
  - 내부 로직은 최소 (Story 5에서 구현)
- `order-web/src/hooks/useOrderStatus.ts`: 빈 Hook 뼈대 (Story 6에서 구현)
- `order-web/src/pages/OrderCreatePage.tsx`: `useOrderCreate`만 import하는 빈 페이지
- `order-web/src/pages/OrderStatusPage.tsx`: `useOrderStatus`만 import하는 빈 페이지
- React Router v6 설치 + `App.tsx` 라우팅 구성
  - `/orders/new` → `OrderCreatePage`
  - `/orders/:id` → `OrderStatusPage`

#### 설계 포인트

- Hook은 `api/` 함수만 호출한다. 직접 `fetch`하지 않는다.
- Page는 Hook만 호출한다. 직접 `api/`를 호출하지 않는다.
- 이 규칙이 Story 5~7에서 지켜져야 3계층 분리가 유지된다.

#### 이 Task에서 하지 않을 것

- 실제 폼 UI (Story 5)
- 페이지 간 navigation 로직 (Story 7)
- 에러 처리 UI (Story 5)

#### 테스트 게이트

- `tsc --noEmit` 통과
- 두 경로(`/orders/new`, `/orders/:id`) 접속 시 각 빈 페이지가 렌더링됨
- 브라우저 뒤로가기·앞으로가기가 SPA 내에서 동작함

#### 머지 조건

Story 5 구현이 새 폴더 생성 없이 이 뼈대에 로직만 채우는 형태로 가능해야 한다.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- `ErrorBoundary` — Epic 2 범위 밖
- 전역 상태 관리 — 필요성이 명확해지기 전까지 미도입
- 공통 UI 컴포넌트 라이브러리 — Story 5에서 실제 UI가 생길 때 판단
- 프론트엔드 테스트(Vitest, React Testing Library) — Epic 2 범위 밖

---

## 5. 권장 순서 요약

1. `types/` 정의 (백엔드 DTO와 1:1 매칭)
2. `api/` 함수 구현 (fetch 래핑)
3. `hooks/` 뼈대 작성 (상태 시그니처만)
4. `pages/` 뼈대 + React Router 구성
