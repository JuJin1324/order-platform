# TODO: API 오류 응답 Body 표준 정의

Story 2 Task 1 작업 중 `@ApiResponse`에 `ProblemDetail` 스키마를 명시했으나,
실제 오류 발생 시 반환되는 Body와 연동되어 있지 않다.
이 문서는 오류 응답 표준을 결정하고 구현 방향을 정리하기 위한 TODO다.

---

## 현재 상태

- `getOrder` — 주문 미존재 시 `IllegalArgumentException` → Spring Boot 기본 핸들러 → **500** 반환
- `createOrder` — `@Valid` 실패 → Spring Boot 기본 핸들러 → **400** 반환 (Body는 기본 형식)
- Swagger 문서에는 404, 400으로 명시했지만 실제 동작과 불일치

---

## 선택지 비교

| 방식 | 예시 | Spring Boot 지원 | React 친화성 |
|------|------|-----------------|-------------|
| **RFC 9457 ProblemDetail** | `{ "type": "...", "title": "Not Found", "status": 404, "detail": "..." }` | 네이티브 (`ProblemDetail` 클래스) | 낮음 — `type`(URI)로 분기해야 함 |
| **커스텀 오류 DTO** | `{ "code": "ORDER_NOT_FOUND", "message": "..." }` | 없음 (직접 구현) | 높음 — `error.code`로 분기 용이 |
| **Google Error Model** | `{ "error": { "code": 404, "status": "NOT_FOUND", "message": "..." } }` | 없음 (직접 구현) | 중간 |

### 권장: 커스텀 오류 DTO

이 프로젝트는 React SPA가 오류를 직접 소비한다.
`error.code === 'ORDER_NOT_FOUND'`로 분기하는 것이 `error.type`(URI) 비교보다 자연스럽다.
RFC 9457은 외부 공용 API나 여러 팀이 소비하는 API에서 적합하다.

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다.",
  "timestamp": "2026-04-20T09:00:00Z"
}
```

---

## 구현 시 필요한 작업

1. **`ErrorResponse` 공통 DTO 정의**
   ```java
   public record ErrorResponse(String code, String message, Instant timestamp) {}
   ```

2. **도메인 예외 클래스 정의**
   - `IllegalArgumentException` 대신 `OrderNotFoundException` 같은 명시적 예외 사용
   - 예외 클래스에 오류 코드(`ORDER_NOT_FOUND`)를 담는 구조

3. **`@RestControllerAdvice` 작성**
   - `OrderNotFoundException` → 404 + `ErrorResponse`
   - `MethodArgumentNotValidException` → 400 + `ErrorResponse` (필드별 검증 실패 포함)

4. **Swagger `@ApiResponse` 수정**
   - 현재 `ProblemDetail`로 명시된 스키마를 `ErrorResponse`로 교체

5. **테스트 추가**
   - 404, 400 응답 Body 구조 검증 (`@WebMvcTest` or `@SpringBootTest`)

---

## 우선순위

React SPA 연동(Story 3) 전에 처리하는 것을 권장한다.
프론트엔드에서 에러 처리 코드를 작성하기 전에 오류 Body 구조가 확정되어야
불필요한 재작업이 생기지 않는다.
