# Story 1 Swagger 기본 연동 Task 계획

이 문서는 "order-service의 REST API를 Swagger UI로 어떻게 노출할 것인가"를 정리한다.

---

## 1. 고정 결정

- `springdoc-openapi` 버전은 Spring Boot 3.x 호환 버전(`springdoc-openapi-starter-webmvc-ui`)을 사용한다.
- Swagger UI 경로는 기본값(`/swagger-ui/index.html`)을 유지한다.
- security 설정 없이 개발 환경에서 접근 가능하도록 둔다.
- OpenAPI 명세 파일(`/v3/api-docs`)도 함께 노출한다. React 개발 시 API 스펙 확인 용도로 쓰인다.

---

## 2. 권장 Task 순서

### Task 1. springdoc-openapi 의존성 추가 + Swagger UI 노출

#### 목표

브라우저에서 `/swagger-ui/index.html`에 접속했을 때 order-service의 REST 엔드포인트가 Swagger UI에 렌더링되는 상태를 만든다.

#### 핵심 작업

- `order-service/build.gradle`에 `springdoc-openapi-starter-webmvc-ui` 의존성 추가
- `order-service/src/main/resources/application.yml`에 springdoc 기본 설정 추가
  - `springdoc.api-docs.path=/v3/api-docs`
  - `springdoc.swagger-ui.path=/swagger-ui.html`
- 기존 Spring Security 또는 CSRF 설정이 있다면 Swagger UI 경로를 허용 목록에 추가

#### 이 Task에서 하지 않을 것

- `@Operation`, `@ApiResponse`, `@Schema` 어노테이션 추가 (Story 2)
- `@OpenAPIDefinition` 글로벌 Info 설정 (Story 2)
- 엔드포인트 설명 보강 (Story 2)

#### 테스트 게이트

- 기존 `order-service` 단위 테스트 전부 통과
- 기존 `scenario-test` 전부 통과

#### 머지 조건

`/swagger-ui/index.html` 접속 시 order-service의 엔드포인트 목록이 보여야 한다.  
어노테이션이 없어 설명이 부족해도 무방하다. 이 Task은 "켜는 것"만 목표로 한다.

---

## 3. 이 계획에서 의도적으로 뒤로 미룬 것

- controller·DTO 어노테이션 — Story 2
- React SPA와의 연동 — Story 3부터
- `OrderStatusHistory` 엔드포인트 문서화 — Story 6에서 엔드포인트 추가 시
- Swagger UI 커스터마이징(테마, 로고 등)
- 운영 환경에서 Swagger UI 비활성화 설정

---

## 4. 권장 순서 요약

1. 의존성 추가
2. springdoc 기본 설정 추가
3. 브라우저에서 Swagger UI 접속 확인
4. Story 2로 넘어가기 전 "무엇이 부족한가" 직접 확인
