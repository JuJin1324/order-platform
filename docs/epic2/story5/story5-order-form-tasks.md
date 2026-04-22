# Story 5 주문 생성 폼 + API 연결 Task 계획

이 문서는 "Story 4의 빈 뼈대에 주문 생성 기능을 실제로 어떻게 채울 것인가"를 정리한다. 이 Story가 머지되면 A 완료 기준(Swagger 완성 + React에서 실제 API 호출 가능)이 달성된다.

---

## 1. 왜 세 Task로 나누는가

Story 5는 Epic 2에서 난이도가 가장 높다. 폼 처리, API 호출, 상태 관리, 에러 처리가 동시에 들어온다. 한 번에 전부 구현하면 문제 발생 시 "어디서 막혔는가"를 분리하기 어렵다.

- **Task 0**: 실행 환경 구성 — `docker compose up -d`로 전체 스택을 올린다
- **Task 1**: Happy path만 먼저 끝까지 연결 — "성공 흐름이 돈다"가 확정
- **Task 2**: 에러 처리 + UX 개선 — 이미 작동하는 흐름 위에 실패 분기를 얹음

이 순서로 가면 Task 1에서 폼 → API → 응답 표시의 기본 회로가 먼저 검증되고, Task 2는 순수하게 "실패 상황에서 UI가 반응하는가"에 집중할 수 있다.

---

## 2. 고정 결정

- 폼 라이브러리 미도입 — `useState`로 필드 상태 관리
- 입력 validation은 최소한만. 백엔드의 400 응답으로 대응
- 주문 생성 후 주문 ID는 화면에 그대로 표시한다. 자동 navigation은 Story 7에서
- 에러 UI는 폼 하단 메시지 수준. 토스트·모달 등 공통 컴포넌트 미도입

---

## 3. 권장 Task 순서

### Task 0. docker compose 전체 스택 구성

#### 목표

`docker compose up -d` 한 번으로 Kafka + 세 서비스가 모두 뜨는 환경을 만든다.

#### 배경

`POST /api/orders`는 order-service가 Kafka에 이벤트를 발행한다. Kafka 없이 order-service만 구동하면 이벤트 발행 시점에 오류가 발생한다. 전체 Saga 흐름(`CREATED → CONFIRMED / CANCELLED`)을 UI에서 확인하려면 세 서비스가 모두 실행 중이어야 한다.

#### 핵심 작업

- `docker compose.yml`에 order-service, payment-service, inventory-service 추가
- 각 서비스가 Kafka 컨테이너에 의존하도록 `depends_on` 설정
- `KAFKA_BOOTSTRAP_SERVERS` 환경 변수를 컨테이너 내부 주소(`kafka:9092`)로 설정

#### 테스트 게이트

```bash
docker compose up -d
```

- 모든 컨테이너 정상 기동 확인 (`docker compose ps`)
- `http://localhost:8081/api/orders/health` 응답 확인

#### 머지 조건

`docker compose up -d` 후 order-service health 엔드포인트가 200을 반환한다.

---

### Task 1. 주문 생성 폼 + Happy Path

#### 목표

주문 생성 페이지에서 폼을 제출하면 `POST /api/orders`가 실제로 호출되고, 응답으로 받은 주문 ID가 화면에 표시되는 상태를 만든다.

#### 핵심 작업

- `hooks/useOrderCreate.ts` 구현
  - `createOrder` 함수: `api/orderApi.ts`의 `createOrder` 호출
  - `isLoading`, `result` 상태 관리
  - `error` 상태는 Task 2에서 채움 (구조만 선언)
- `pages/OrderCreatePage.tsx`에 폼 UI 추가
  - 필드: Swagger 문서에서 확인된 최소 필드 (`sku`, `quantity`, `amount`)
  - 제출 버튼 + 로딩 표시
  - 성공 응답의 주문 ID를 화면에 출력

#### 설계 포인트

- Hook은 `api/` 함수만 호출한다. 컴포넌트에서 `fetch`가 등장하면 3계층 위반
- 로딩 중에는 제출 버튼 `disabled`로 중복 클릭 방지 (최소한)

#### 이 Task에서 하지 않을 것

- 에러 메시지 UI (Task 2)
- 제출 버튼 중복 클릭 방지 고도화 (Task 2)
- 성공 후 상태 조회 페이지로 navigation (Story 7)
- 폼 validation 라이브러리 도입

#### 테스트 게이트

- 브라우저에서 폼 제출 → DevTools Network 탭에 `POST /api/orders` 확인
- 응답 JSON의 `orderId`가 화면에 표시됨
- order-service DB에서 주문 레코드 실제 생성 확인
- Swagger UI에서 동일 요청을 직접 보냈을 때와 같은 결과

#### 머지 조건

"React 폼으로 주문이 생성되고 ID가 보인다"가 확인된 상태. 실패 상황은 아직 처리하지 않는다.

---

### Task 2. 에러 처리 + UX 개선

#### 목표

실패 상황(네트워크 오류, 400, 500 등)에서 사용자가 원인을 알 수 있게 UI를 정비한다.

#### 핵심 작업

- `useOrderCreate.ts`의 에러 상태 구현
  - `api/orderApi.ts`에서 throw된 에러를 `error` 상태에 담음
  - 재제출 시 이전 에러 초기화
- 폼 UI에 에러 메시지 표시
  - HTTP status code별 구분 메시지 (최소 400, 500)
  - 네트워크 오류 메시지
- UX 디테일
  - 제출 중 중복 클릭 방지 확실히
  - 성공 후 폼 필드 초기화 (주문 ID는 남김)

#### 설계 포인트

- 에러는 Hook에서 캐치하고 `error` 상태로 노출한다
- "어떤 메시지를 어떻게 보여줄지"는 컴포넌트가 결정한다 (Hook은 원시 에러만 노출)

#### 이 Task에서 하지 않을 것

- 에러 모달·토스트 등 공통 UI — 페이지 내 메시지로 한정
- 자동 재시도 — 사용자가 수동으로 다시 제출하도록 둠
- 상세 validation — 백엔드 400 메시지 표시 수준이면 충분

#### 테스트 게이트

- DevTools로 네트워크 차단 → 에러 메시지 표시 확인
- 잘못된 요청(예: 음수 수량) → 400 응답 메시지 표시
- 에러 후 재제출 → 정상 동작 확인

#### 머지 조건

성공·실패 시나리오 모두에서 UI가 "무엇이 일어났는지" 전달하는 상태. "에러가 나면 아무 반응이 없다"가 없어야 한다.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- 상태 조회 화면 — Story 6
- 주문 생성 후 상태 조회로 자동 이동 — Story 7
- React Hook Form 등 폼 라이브러리 — Epic 2 범위 밖
- `ErrorBoundary`, 에러 타입 정규화 — Epic 2 범위 밖
- 접근성(aria-live 등) — Epic 2 범위 밖

---

## 5. 권장 순서 요약

1. docker compose.yml에 세 서비스 추가 + 전체 스택 기동 확인
2. `useOrderCreate` Hook happy path 구현
3. 폼 UI 추가 + API 연결 확인
4. 주문 ID 화면 표시 + DB 레코드 확인
5. 에러 상태 구현
6. 에러 메시지 UI 추가
7. UX 디테일 정리 (중복 클릭 방지, 메시지 초기화)
