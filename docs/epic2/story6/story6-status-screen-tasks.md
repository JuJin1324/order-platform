# Story 6 주문 상태 조회 화면 Task 계획

이 문서는 Saga의 비동기 상태 전이를 UI 타임라인으로 시각화하는 작업을 정리한다. Epic 1(백엔드 Saga)과 Epic 2(풀스택)가 가장 직접적으로 연결되는 지점이다.

---

## 1. 왜 두 Task로 나누는가

Story 6은 백엔드 변경(이력 테이블 + API 추가)과 프론트엔드 변경(타임라인 화면)이 같은 Story에 묶여 있다. 두 변경은 검토 이야기가 명백히 다르다.

- **Task 1 (백엔드)**: "상태 전이가 발생할 때마다 이력이 기록되고 API로 조회된다"
- **Task 2 (프론트엔드)**: "API가 돌려준 이력이 화면에 타임라인으로 보인다"

Task 1은 Swagger UI와 백엔드 테스트만으로 단독 검증이 가능하다. Task 2는 Task 1이 머지된 뒤 그 위에 붙는다. 한 PR에 묶으면 백엔드 데이터 모델 결정과 프론트엔드 렌더링 결정이 섞여 검토가 흐려진다.

프론트엔드 내부에서 "해피 패스"와 "에러 처리"를 더 쪼갤 수도 있지만, 이번엔 묶었다. 이유는 Story 6의 에러 처리가 "존재하지 않는 주문 ID" 한 케이스에 한정되어 있고, 타임라인 렌더링과 한 번에 검토해도 충분히 작은 변경 폭이기 때문이다.

---

## 2. 고정 결정

- `OrderStatusHistory`는 **감사 로그(audit log)**다. 이벤트 소싱이 아니다. `Order` 엔티티의 현재 `status` 컬럼은 그대로 유지하고, 이력은 별도 테이블에 누적한다
- 이력 삽입은 AOP(`@Aspect`)로 상태 전이 메서드를 가로채 별도 트랜잭션(`REQUIRES_NEW`)에서 수행한다. 도메인 서비스가 이력 저장 책임을 직접 알지 않고, 이력 삽입 실패가 주문 상태 전이를 롤백시키지 않는다
- 자동 갱신(polling, SSE) 미도입 — 수동 새로고침 버튼만. Saga의 비동기 흐름을 의도적으로 관찰하기 위함 (Epic 2 범위 결정)
- 주문 ID 입력 방식은 폼 텍스트 입력 + 조회 버튼. URL 경로 진입은 Story 7 범위
- 타임라인 UI는 단순 리스트 (status + changedAt). 차트·아이콘 미도입

---

## 3. 권장 Task 순서

### Task 1. 백엔드 — OrderStatusHistory + 이력 삽입 + 조회 API

#### 목표

상태 전이가 발생할 때마다 `OrderStatusHistory`에 한 줄이 쌓이고, `GET /orders/{id}/status-history`로 시간순 이력 배열을 조회할 수 있는 상태를 만든다.

#### 핵심 작업

- 엔티티/리포지토리
  - `OrderStatusHistory` 엔티티: `id`, `orderId`, `status`, `changedAt`
  - JPA 리포지토리 + `findByOrderIdOrderByChangedAtAsc` 류 조회 메서드
  - `orderId` 컬럼에 인덱스 추가 (`@Index`)
- 이력 삽입 — AOP 방식
  - `OrderStatusHistoryAspect`: 상태 전이 메서드(`processOrder`, `confirmOrder`, `cancelOrder`)에 `@AfterReturning`으로 이력 삽입
  - `@Transactional(propagation = REQUIRES_NEW)`으로 별도 트랜잭션에서 실행
  - 도메인 서비스는 이력 저장 코드를 포함하지 않는다
- 조회 엔드포인트
  - `GET /orders/{id}/status-history` 추가
  - 응답: `[{status, changedAt}, ...]` 시간순 정렬
  - 존재하지 않는 `orderId` → 빈 배열 반환 (404 대신 — 이력이 없는 것과 주문이 없는 것을 구분하지 않음)
- Story 2 기준으로 `@Operation`, `@ApiResponse`, `@Schema` 주석 추가

#### 설계 포인트

- `Order` 엔티티의 `status` 컬럼은 변경하지 않는다. 현재 상태는 기존 그대로, 이력은 별도 테이블이라는 분리 유지
- AOP로 이력 삽입을 분리하면 도메인 서비스가 `OrderStatusHistoryRepository`를 알 필요가 없다. 이력 기록 방식이 바뀌어도 도메인 서비스는 수정하지 않는다
- `REQUIRES_NEW`로 이력 삽입이 실패해도 주문 상태 전이는 커밋된다

#### 이 Task에서 하지 않을 것

- 프론트엔드 변경 일체 (Task 2)
- 자동 갱신 메커니즘
- 이력 페이징·필터
- 이벤트 소싱 전환

#### 테스트 게이트

- 기존 전체 테스트 통과
- 신규 단위 테스트
  - `OrderStatusHistoryAspect`: 상태 전이 메서드 호출 시 이력 삽입 메서드가 호출되는지 검증
  - 이력 삽입 실패 시 도메인 트랜잭션이 롤백되지 않는지 검증
- 신규 통합 테스트
  - 주문 생성 → 이력 1건(`CREATED`) 확인
  - `confirmOrder` 호출 → 이력 2건(`CREATED`, `CONFIRMED`) 시간순 확인
  - `cancelOrder` 호출 → 이력 2건(`CREATED`, `CANCELLED`) 시간순 확인
  - 존재하지 않는 `orderId` 조회 → 빈 배열 반환
- Swagger UI에서 신규 엔드포인트 직접 호출해 응답 형태 확인

#### 머지 조건

"Saga 흐름이 한 번 돈 뒤 Swagger UI에서 이력을 조회하면 시간순으로 두 줄이 보인다"가 확인된 상태.

---

### Task 2. 프론트엔드 — 타임라인 화면 + 새로고침 + 에러 처리

#### 목표

주문 ID를 입력하면 이력이 시간순 타임라인으로 보이고, 새로고침 버튼으로 최신 이력을 다시 가져올 수 있는 화면을 만든다. 존재하지 않는 ID에 대해 사용자에게 피드백이 표시된다.

#### 핵심 작업

- API/타입
  - `api/orderApi.ts`에 `OrderStatusHistory` 타입 추가 (`status`, `changedAt`)
  - `getOrderStatusHistory(orderId)` 함수 추가
- Hook
  - `hooks/useOrderStatus.ts`에 이력 조회 로직 추가
  - 상태: `history`, `isLoading`, `error`
  - 액션: `fetchHistory(orderId)` (조회), `refresh()` (마지막 조회 ID 재조회)
- 페이지
  - `pages/OrderStatusPage.tsx`
    - 주문 ID 입력 필드 + 조회 버튼
    - 이력 배열을 `{status, changedAt}` 리스트로 시간순 렌더링
    - 새로고침 버튼: 마지막 조회 ID로 재호출
    - 404 응답 시 "해당 주문을 찾을 수 없습니다" 메시지 표시
    - 네트워크 오류 메시지 (Story 5 Task 2와 같은 결)

#### 설계 포인트

- 컴포넌트는 `useOrderStatus`가 노출하는 값/함수만 사용한다. 직접 `fetch` 등장 시 3계층 위반
- "새로고침"은 Hook 내부에 마지막 조회 ID를 보관해두고 재호출하는 방식. 컴포넌트가 ID를 다시 넘기지 않아도 동작하게
- 타임라인 항목 정렬 방식(오래된 → 최신 vs. 최신 → 오래된)을 한 가지로 결정해 일관 적용

#### 이 Task에서 하지 않을 것

- 자동 갱신 — 수동 새로고침만
- URL 경로 파라미터 진입 (Story 7)
- 차트·아이콘 등 시각적 강화
- 토스트·모달 — 페이지 내 메시지로 한정

#### 테스트 게이트

- Task 1이 머지된 상태에서 docker compose 환경 기동
- Story 5에서 생성한 주문 ID 입력 → `CREATED` 한 줄 표시
- 잠시 대기 후 새로고침 → `CONFIRMED` 추가 확인
- 재고 없는 시나리오 주문 → 새로고침 → `CANCELLED` 추가 확인
- 존재하지 않는 ID 입력 → 에러 메시지 표시
- DevTools로 네트워크 차단 → 네트워크 오류 메시지 표시

#### 머지 조건

"주문 ID를 입력하면 Saga가 만든 상태 전이가 시간순으로 화면에 그대로 찍히고, 잘못된 입력에는 메시지가 보인다"가 확인된 상태.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- 주문 생성 → 상태 조회 자동 이동 (Story 7)
- 자동 폴링·SSE — Epic 2 범위 밖
- 이력 페이징·필터·검색 — Epic 2 범위 밖
- 이벤트 소싱 전환 — 도메인 모델 변경 비용이 커서 의도적으로 배제
- 접근성·시각화 강화 — Epic 2 범위 밖

---

## 5. 권장 순서 요약

1. `OrderStatusHistory` 엔티티/리포지토리 추가
2. 도메인 서비스에서 이력 삽입 호출 추가 (생성·confirm·cancel)
3. `GET /orders/{id}/status-history` 엔드포인트 + Swagger 주석
4. 백엔드 테스트 통과 → Task 1 머지
5. `api/orderApi.ts`에 타입/함수 추가
6. `useOrderStatus` Hook에 이력 조회·새로고침 구현
7. 페이지에 입력 폼 + 타임라인 + 에러 메시지
8. 시연 시나리오 통과 → Task 2 머지
