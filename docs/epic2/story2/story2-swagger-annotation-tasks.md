# Story 2 Swagger 어노테이션 정리 Task 계획

이 문서는 "Story 1에서 기본 노출만 된 Swagger UI를 어떻게 명세서 수준의 API 문서로 올릴 것인가"를 정리한다.

---

## 1. 왜 세 Task로 나누는가

어노테이션 작업은 관점이 다른 세 종류로 나뉜다.

- **Task 1**: controller 어노테이션 — 엔드포인트의 의도·응답 계약을 드러낸다 (API 행동 의미)
- **Task 2**: DTO `@Schema` 어노테이션 — 요청/응답 필드의 역할·예시를 드러낸다 (데이터 구조 의미)
- **Task 3**: 오류 응답 표준화 — Swagger에 명시한 오류 스키마와 실제 반환 Body를 일치시킨다

Task 1·2는 "문서를 채우는 작업"이고, Task 3은 "문서와 코드의 불일치를 제거하는 작업"이다. Task 3은 Story 3(React 연동) 전에 반드시 완료해야 한다. 프론트엔드에서 오류 처리 코드를 작성하기 전에 오류 Body 구조가 확정되어야 불필요한 재작업이 생기지 않는다.

---

## 2. 고정 결정

- 어노테이션은 presentation 계층(controller, DTO)에만 추가한다. 도메인·서비스 계층에는 넣지 않는다.
- `@ApiResponse`는 실제 발생 가능한 status code만 선언한다. 가상의 오류 케이스는 나열하지 않는다.
- `@OpenAPIDefinition`으로 API 전체 메타데이터(title, version)를 한 번만 설정한다.
- `@Schema`는 요청/응답 DTO에만 추가한다. JPA 엔티티에는 붙이지 않는다.
- 모든 controller는 `produces`/`consumes`를 명시한다. Swagger UI에서 `*/*` 대신 `application/json`이 표시되도록 하고, 의도하지 않은 미디어 타입 수락을 방지한다.
  - 응답: `@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)` — 클래스 레벨에 선언해 전체 엔드포인트에 적용
  - 요청 Body가 있는 엔드포인트: `@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)`
  - `@ApiResponse`의 오류 응답 `@Content`에도 `mediaType`을 명시한다. `produces` 설정은 정상 응답 경로에만 적용되고 `@Content`에는 전파되지 않는다.
- `springdoc.override-with-generic-response: false`를 설정한다. 기본값(`true`)은 `@RestControllerAdvice`의 `@ExceptionHandler`를 모든 엔드포인트에 자동으로 추가한다. `false`로 설정하면 각 엔드포인트에 명시한 `@ApiResponse`만 표시된다.

---

## 3. 권장 Task 순서

### Task 1. Controller 어노테이션

#### 목표

각 엔드포인트의 의도와 응답 계약을 Swagger UI에서 바로 읽을 수 있는 상태로 만든다.

#### 핵심 작업

- `@OpenAPIDefinition`으로 API 전체 title, version, description 선언
- 각 controller 메서드에 `@Operation(summary, description)` 추가
  - `summary`: 한 줄 요약 (예: "주문 생성")
  - `description`: 호출 목적, 비동기 여부, 반환 시점 등 행동 맥락
- 각 엔드포인트에 `@ApiResponse` 추가
  - 정상 응답 status code와 반환 타입 명시
  - 실제 발생 가능한 오류 케이스(400, 404 등)

#### 이 Task에서 하지 않을 것

- DTO 필드 `@Schema` 추가 (Task 2)
- 엔드포인트 시그니처·응답 스펙 변경
- 비즈니스 로직 수정

#### 테스트 게이트

- 기존 `order-service` 단위/integration 테스트 통과
- 기존 `scenario-test` 통과

#### 머지 조건

Swagger UI에서 각 엔드포인트의 summary, description, 응답 케이스가 모두 채워져 있다. 엔드포인트 이름만 보고도 목적이 파악되는 상태.

---

### Task 2. DTO `@Schema` 어노테이션

#### 목표

Swagger UI의 "Schemas" 섹션에서 모든 DTO 필드가 설명과 예시를 가진 상태를 만든다.

#### 핵심 작업

- 요청 DTO 필드에 `@Schema(description, example)` 추가
  - `description`: 필드 역할 (예: "주문할 상품 ID")
  - `example`: 실제 사용 가능한 예시 값
- 응답 DTO 필드에도 동일하게 추가
- enum 타입(예: `OrderStatus`)은 enum 자체에 설명을 붙이거나 `@Schema(allowableValues)`로 명시

#### 이 Task에서 하지 않을 것

- DTO 필드 추가·삭제·타입 변경
- controller 어노테이션 재조정 (Task 1에서 완료)

#### 테스트 게이트

- 기존 `order-service` 테스트 통과
- 기존 `scenario-test` 통과

#### 머지 조건

Swagger UI를 처음 본 사람이 DTO 필드를 보고 "이게 뭐냐"는 질문을 하지 않는 수준. 모든 필드에 설명과 예시가 있어야 한다.

---

### Task 3. API 오류 응답 표준화

> 배경: Task 1에서 404·400 응답에 `ProblemDetail` 스키마를 명시했으나 실제 동작과 불일치한다.
> 상세 배경은 [`story2-api-error-response-standard.md`](./story2-api-error-response-standard.md) 참고.

#### 목표

오류 발생 시 실제 반환되는 Body 구조를 확정하고, Swagger 문서와 일치시킨다.

#### 핵심 작업

- `ErrorResponse` 공통 오류 DTO 정의 (`code`, `message`, `timestamp`)
- 도메인 예외 클래스 정의 (`OrderNotFoundException` 등)
- `@RestControllerAdvice` 작성
  - `OrderNotFoundException` → 404 + `ErrorResponse`
  - `MethodArgumentNotValidException` → 400 + `ErrorResponse`
- `@ApiResponse` 스키마를 `ProblemDetail`에서 `ErrorResponse`로 교체
- 오류 응답 Body 구조 검증 테스트 추가

#### 이 Task에서 하지 않을 것

- payment-service, inventory-service 동일 적용 (order-service에만 집중)
- 비즈니스 로직 수정

#### 테스트 게이트

- 기존 `order-service` 테스트 통과
- 404, 400 응답 Body 구조 검증 테스트 추가 및 통과

#### 머지 조건

`POST /api/orders`에 잘못된 요청을 보냈을 때와 `GET /api/orders/{없는ID}`를 호출했을 때,
Swagger에 문서화된 스키마(`ErrorResponse`)와 동일한 구조의 Body가 반환된다.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- Swagger UI 테마·로고 커스터마이징
- `@ExampleObject` 기반 요청/응답 예시 세밀 조정
- `@Tag`로 컨트롤러 그룹핑 — order-service는 컨트롤러가 적어 불필요
- 인증·보안 스키마 문서화 — Epic 2 범위 밖

---

## 5. 권장 순서 요약

1. Controller 어노테이션으로 API 의도 노출
2. Swagger UI에서 "DTO 필드가 뭔지 모르겠다" 부족함 확인
3. DTO `@Schema`로 필드 의미 채우기
4. 오류 응답 표준화 — Swagger 문서와 실제 동작 일치
5. "처음 보는 사람도 React에서 호출할 수 있는가" 최종 검토
