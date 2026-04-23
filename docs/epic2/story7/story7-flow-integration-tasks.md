# Story 7 전체 흐름 연결 Task 계획

이 문서는 Story 5(주문 생성)와 Story 6(상태 조회) 두 화면을 하나의 SPA 사용자 흐름으로 잇는 작업을 정리한다. 이 Story가 머지되면 Epic 2의 B 완료 기준(주문 생성 → 상태 조회까지 동작하는 풀스택 데모)이 달성된다.

---

## 1. 왜 네 Task로 나누는가

Story 7의 코드 변경 자체는 작다. 하지만 검토 이야기가 세 개로 갈린다.

- **Task 1**: "생성 → 자동 이동 → 자동 조회"의 해피 패스 회로가 한 번 끝까지 돈다
- **Task 2**: 그 회로 위에 SPA로서 깨지지 말아야 할 예외 동작(뒤로가기, URL 직접 입력, 실패 시나리오)을 얹는다
- **Task 3**: 기능과 무관하게 포트폴리오 목적의 UI 스타일링을 분리해서 얹는다
- **Task 4**: 전체 스택을 `make up` 한 번으로 실행할 수 있도록 order-web을 컨테이너화한다

Task 1을 먼저 끝내면 "이동 자체가 되는가"를 단독으로 검증할 수 있다. Task 2는 이미 도는 흐름 위에서 "예외 입력에서도 깨지지 않는가"에 집중한다. Task 3은 기능 검증이 완료된 뒤 스타일만 다루므로 기능 회귀 위험 없이 진행할 수 있다. Task 4는 스타일까지 확정된 최종 결과물을 컨테이너로 패키징한다.

---

## 2. 고정 결정

- 라우팅 도구는 Story 4에서 도입한 React Router를 그대로 사용한다 — 추가 라이브러리 없음
- 주문 ID 전달 방식은 **URL 경로 파라미터**(`/orders/:orderId/status`)로 한다. 쿼리스트링·라우터 state 미사용. 이유: 새로고침·직접 URL 입력에서도 동일하게 동작하는 단일 진입 경로가 필요
- 자동 조회는 페이지 마운트 시점에 1회만 실행한다. polling·웹소켓 도입 없음
- 데모 시연용 보조 UI(예: 진행 안내 토스트) 미도입 — 기존 화면 그대로 이어 붙인다

---

## 3. 권장 Task 순서

### Task 1. 생성 → 조회 라우팅 연결 + URL 기반 자동 조회

#### 목표

주문 생성 성공 시 상태 조회 페이지로 자동 이동하고, 이동된 페이지가 URL의 주문 ID로 이력을 자동 조회해서 보여주는 상태를 만든다.

#### 핵심 작업

- 라우터 경로 정비
  - 상태 조회 페이지 경로를 `/orders/:orderId/status`로 등록
  - 기존 Story 6의 "주문 ID 수동 입력" 진입 경로는 유지 또는 통합 (둘 중 어느 쪽이든 명확하게 결정)
- `OrderCreatePage.tsx`
  - 주문 생성 성공 응답 수신 직후 `navigate(\`/orders/${orderId}/status\`)` 호출
  - 이동 직전 로딩 표시는 그대로 유지 (사용자가 화면 점프를 인지할 수 있게)
- `OrderStatusPage.tsx`
  - `useParams`로 `orderId`를 읽어 `useOrderStatus`에 주입
  - 마운트 시점에 자동으로 이력 조회 (새로고침 버튼은 그대로 유지)
- `hooks/useOrderStatus.ts`
  - 이미 받은 `orderId`로 마운트 시 조회를 실행하는 분기 추가 (수동 조회 경로와 충돌하지 않게)

#### 설계 포인트

- 자동 조회는 `useEffect`에서 한 번만 트리거한다. `orderId`가 바뀔 때만 재실행
- 컴포넌트는 `useParams` 결과를 Hook에 넘기기만 한다. URL 파싱 책임이 Hook으로 새지 않게

#### 이 Task에서 하지 않을 것

- 뒤로가기·직접 URL 입력 시 예외 동작 보완 (Task 2)
- 존재하지 않는 주문 ID 처리 강화 (Task 2 — Story 6에서 이미 기본 에러 처리는 있음)
- 데모 시연용 안내 UI

#### 테스트 게이트

- 기존 전체 테스트 통과
- 브라우저에서 폼 제출 → 응답 수신 직후 URL이 `/orders/{id}/status`로 변경
- 이동된 페이지가 자동으로 이력 조회 요청을 보내고 첫 이력(`CREATED`)을 표시
- DevTools Network 탭에서 `POST /api/orders` → `GET /api/orders/{id}/status-history` 순서 확인

#### 머지 조건

"폼 제출 한 번으로 화면이 이력 페이지까지 자동 도달한다"가 확인된 상태. 예외 입력 보완은 다음 Task로 넘긴다.

---

### Task 2. SPA 예외 동작 보완 + 시연 시나리오 검증

#### 목표

Task 1의 해피 패스 위에, 뒤로가기·직접 URL 입력·실패 시나리오에서도 깨지지 않는 SPA 동작을 확정한다. 이 Task가 끝나면 클라이언트 데모로 그대로 보여줄 수 있는 상태가 된다.

#### 핵심 작업

- 뒤로가기 동작 점검
  - 상태 조회 페이지 → 뒤로가기 → 주문 생성 페이지로 복귀
  - 복귀된 폼 상태 결정: 초기화 vs. 직전 입력 유지 중 한쪽으로 명시 (기본값: 초기화 — 새 주문은 새로 입력)
- 직접 URL 진입 동작 점검
  - 주소창에 `/orders/{유효한 ID}/status` 입력 → 자동 조회 동작
  - `/orders/{없는 ID}/status` 입력 → Story 6의 에러 메시지 표시
  - `/orders//status` 등 비정상 URL → 안전한 폴백(404 또는 입력 페이지)
- 실패 시나리오 시연 가능성 확보
  - `CONFIRMED` 시나리오: 정상 주문 → 자동 이동 → 새로고침 시 `CONFIRMED` 추가 확인
  - `CANCELLED` 시나리오: 재고 없는 케이스 → 자동 이동 → 새로고침 시 `CANCELLED` 추가 확인
  - 두 시나리오 모두 별도 코드 변경 없이 데이터/환경만으로 재현 가능해야 함

#### 설계 포인트

- 새 컴포넌트 추가는 최소화한다. 기존 화면의 분기로 처리
- 폴백 라우트(`*` route)가 없다면 이번에 추가 — 예상치 못한 URL이 빈 화면으로 끝나지 않게

#### 이 Task에서 하지 않을 것

- 자동 폴링·실시간 갱신 (Epic 2 범위 밖)
- 데모용 진행 상태 토스트·아이콘 (필요 시 Story 8 포트폴리오 단계에서 결정)
- 라우팅 가드·인증 흐름

#### 테스트 게이트

- 기존 전체 테스트 통과
- 사용자 직접 시연 체크리스트 통과
  - [ ] 폼 제출 → 자동 이동 → `CREATED` 표시
  - [ ] 잠시 후 새로고침 → `CONFIRMED` 추가
  - [ ] 재고 없는 시나리오 재현 → 새로고침 → `CANCELLED` 추가
  - [ ] 상태 페이지에서 뒤로가기 → 주문 생성 페이지 복귀
  - [ ] 주소창에 `/orders/{유효 ID}/status` 직접 입력 → 정상 조회
  - [ ] 주소창에 `/orders/없는ID/status` 직접 입력 → 에러 메시지 표시
  - [ ] 비정상 URL 진입 → 폴백 화면 동작

#### 머지 조건

위 시연 체크리스트가 한 번에 통과된 상태. "데모 중 어느 한 가지가 어색하거나 깨진다"가 없어야 한다.

---

### Task 3. Tailwind CSS 적용 — 포트폴리오 UI 정비

#### 목표

포트폴리오에 첨부할 수 있는 수준으로 두 페이지의 UI를 Tailwind CSS로 정비한다. 기능 동작은 Task 2에서 이미 검증됐으므로 이 Task는 시각적 완성도에만 집중한다.

#### 핵심 작업

- Tailwind CSS 설치 및 설정
  - `npm install tailwindcss @tailwindcss/vite`
  - `vite.config.ts`에 Tailwind 플러그인 추가
  - `index.css`에 `@import "tailwindcss"` 추가
- `OrderCreatePage.tsx` 스타일 적용
  - 폼 레이아웃, 입력 필드, 버튼, 결과 표시 영역
- `OrderStatusPage.tsx` 스타일 적용
  - 조회 폼, 타임라인 리스트, 새로고침 버튼, 에러 메시지

#### 이 Task에서 하지 않을 것

- 공통 컴포넌트 추출 (Button, Input 등) — 오버엔지니어링
- 다크모드, 반응형 완전 대응 — Epic 2 범위 밖
- 애니메이션·트랜지션

#### 테스트 게이트

- 기존 기능 동작 이상 없음 (Task 2 시연 체크리스트 재확인)
- 두 페이지가 포트폴리오 첨부 가능한 수준으로 렌더링됨

#### 머지 조건

화면 캡처를 Story 8 포트폴리오 문서에 첨부할 수 있는 상태.

---

### Task 4. order-web Docker 컨테이너화

#### 목표

`make up` 한 번으로 백엔드 서비스와 함께 order-web도 컨테이너로 실행되는 환경을 만든다. 로컬에 Node.js 없이도 전체 스택을 실행할 수 있게 한다.

#### 핵심 작업

- `order-web/Dockerfile` 추가
  - 멀티 스테이지 빌드: Node.js로 `npm run build` → nginx로 정적 파일 서빙
  - `nginx.conf`: SPA 라우팅을 위해 모든 경로를 `index.html`로 fallback
- `docker-compose.yml`에 `order-web` 서비스 추가
  - 포트: `5173:80` (Vite 개발 서버와 동일한 포트로 노출)
  - `depends_on`: order-service
- `Makefile` `up` 타겟 — 기존 `./gradlew bootJar` 앞에 `npm run build` 추가 여부 결정
  - order-web은 Docker 빌드 시 내부에서 빌드하므로 Makefile 변경 불필요

#### 설계 포인트

- 프로덕션 빌드(`npm run build`)는 Vite 개발 서버 없이 정적 파일로 서빙된다. `/api/*` 프록시는 Vite 개발 서버의 기능이므로 nginx에서 별도로 설정해야 한다
- nginx에서 `/api/*` 요청을 `order-service:8081`로 프록시 패스 설정

#### 이 Task에서 하지 않을 것

- HTTPS 설정 — Epic 2 범위 밖
- CDN·환경별 빌드 분리

#### 테스트 게이트

```bash
make up
```

- `http://localhost:5173` 접속 시 order-web이 컨테이너에서 정상 서빙됨
- 폼 제출 → API 호출이 nginx 프록시를 통해 order-service로 전달됨

#### 머지 조건

`make up` 후 브라우저에서 전체 흐름(주문 생성 → 상태 조회)이 컨테이너 환경에서 동작한다.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- 자동 폴링·SSE·웹소켓 — Epic 2 범위 밖, 의도적으로 수동 새로고침 유지
- 데모 시연 안내 UI(토스트, 진행 표시) — 필요 판단은 Story 8에서
- 인증·권한 — Epic 2 범위 밖
- 포트폴리오용 화면 캡처·서술 — Story 8

---

## 5. 권장 순서 요약

1. 라우터에 `/orders/:orderId/status` 경로 정비
2. `OrderCreatePage`에서 성공 시 `navigate` 호출
3. `OrderStatusPage`에서 `useParams` → 자동 조회 분기 추가
4. 해피 패스 1회 통과 확인 → Task 1 머지
5. 뒤로가기·직접 URL 진입·폴백 라우트 보완
6. `CONFIRMED`/`CANCELLED` 두 시나리오 시연 체크리스트 통과 → Task 2 머지
7. Tailwind CSS 설치 + 두 페이지 스타일 적용 → Task 3 머지
8. order-web Dockerfile + nginx 설정 + docker-compose 추가 → Task 4 머지
