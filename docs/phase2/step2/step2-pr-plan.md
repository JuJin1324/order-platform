# Step 2 Swagger 어노테이션 정리 PR 계획

이 문서는 "Step 1에서 기본 노출만 된 Swagger UI를 어떻게 명세서 수준의 API 문서로 올릴 것인가"를 정리한다.

---

## 1. 왜 두 PR로 나누는가

어노테이션 작업은 관점이 다른 두 종류로 나뉜다.

- **PR 1**: controller 어노테이션 — 엔드포인트의 의도·응답 계약을 드러낸다 (API 행동 의미)
- **PR 2**: DTO `@Schema` 어노테이션 — 요청/응답 필드의 역할·예시를 드러낸다 (데이터 구조 의미)

하나의 PR에 섞으면 리뷰 시 "의도 설명이 충분한가"와 "필드 설명이 충분한가"를 동시에 판단해야 해서 초점이 분산된다. PR 1을 먼저 머지하고 Swagger UI를 직접 확인한 뒤, "DTO 필드가 무슨 뜻인지 모르겠다"는 부족함을 PR 2에서 채우는 순서가 자연스럽다.

---

## 2. 고정 결정

- 어노테이션은 presentation 계층(controller, DTO)에만 추가한다. 도메인·서비스 계층에는 넣지 않는다.
- `@ApiResponse`는 실제 발생 가능한 status code만 선언한다. 가상의 오류 케이스는 나열하지 않는다.
- `@OpenAPIDefinition`으로 API 전체 메타데이터(title, version)를 한 번만 설정한다.
- `@Schema`는 요청/응답 DTO에만 추가한다. JPA 엔티티에는 붙이지 않는다.

---

## 3. 권장 PR 순서

### PR 1. Controller 어노테이션

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

#### 이 PR에서 하지 않을 것

- DTO 필드 `@Schema` 추가 (PR 2)
- 엔드포인트 시그니처·응답 스펙 변경
- 비즈니스 로직 수정

#### 테스트 게이트

- 기존 `order-service` 단위/integration 테스트 통과
- 기존 `scenario-test` 통과

#### 머지 조건

Swagger UI에서 각 엔드포인트의 summary, description, 응답 케이스가 모두 채워져 있다. 엔드포인트 이름만 보고도 목적이 파악되는 상태.

---

### PR 2. DTO `@Schema` 어노테이션

#### 목표

Swagger UI의 "Schemas" 섹션에서 모든 DTO 필드가 설명과 예시를 가진 상태를 만든다.

#### 핵심 작업

- 요청 DTO 필드에 `@Schema(description, example)` 추가
  - `description`: 필드 역할 (예: "주문할 상품 ID")
  - `example`: 실제 사용 가능한 예시 값
- 응답 DTO 필드에도 동일하게 추가
- enum 타입(예: `OrderStatus`)은 enum 자체에 설명을 붙이거나 `@Schema(allowableValues)`로 명시

#### 이 PR에서 하지 않을 것

- DTO 필드 추가·삭제·타입 변경
- controller 어노테이션 재조정 (PR 1에서 완료)

#### 테스트 게이트

- 기존 `order-service` 테스트 통과
- 기존 `scenario-test` 통과

#### 머지 조건

Swagger UI를 처음 본 사람이 DTO 필드를 보고 "이게 뭐냐"는 질문을 하지 않는 수준. 모든 필드에 설명과 예시가 있어야 한다.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- Swagger UI 테마·로고 커스터마이징
- `@ExampleObject` 기반 요청/응답 예시 세밀 조정
- `@Tag`로 컨트롤러 그룹핑 — order-service는 컨트롤러가 적어 불필요
- 인증·보안 스키마 문서화 — Phase 2 범위 밖

---

## 5. 권장 순서 요약

1. Controller 어노테이션으로 API 의도 노출
2. Swagger UI에서 "DTO 필드가 뭔지 모르겠다" 부족함 확인
3. DTO `@Schema`로 필드 의미 채우기
4. "처음 보는 사람도 React에서 호출할 수 있는가" 최종 검토
