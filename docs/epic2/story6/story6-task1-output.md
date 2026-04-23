# Story 6 Task 1 산출물 — 백엔드: OrderStatusHistory + 이력 삽입(AOP) + 조회 API

---

## 완료한 작업

### `order-service/build.gradle` — AOP 의존성 추가

```groovy
implementation 'org.springframework.boot:spring-boot-starter-aspectj'
```

Spring Boot 4.0에서 `spring-boot-starter-aop`가 `spring-boot-starter-aspectj`로 이름이 바뀌었다.

---

### `domain/OrderStatusHistory.java` — 엔티티

```java
@Entity
@Table(name = "order_status_history", indexes = @Index(columnList = "orderId"))
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    public static OrderStatusHistory record(String orderId, OrderStatus status) { ... }
}
```

`orderId` 컬럼에 `@Index`를 추가해 빈번한 조회 쿼리의 성능을 확보했다.

---

### `domain/OrderStatusHistoryRepository.java` — JPA 리포지토리

```java
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(String orderId);
}
```

---

### `application/OrderStatusHistoryAspect.java` — AOP 이력 삽입

```java
@Aspect
@Component
public class OrderStatusHistoryAspect {

    @AfterReturning(
            pointcut = "execution(* com.ordersaga.order.application.OrderApplicationService.createOrder(..)) ||" +
                       "execution(* com.ordersaga.order.application.OrderApplicationService.confirmOrder(..)) ||" +
                       "execution(* com.ordersaga.order.application.OrderApplicationService.cancelOrder(..))",
            returning = "result"
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHistory(OrderResult result) {
        orderStatusHistoryRepository.save(
                OrderStatusHistory.record(result.orderId(), result.status())
        );
    }
}
```

`OrderApplicationService`가 `OrderStatusHistoryRepository`를 직접 알지 않는다. 도메인 로직 변경 없이 이력 기록이 추가됐다. `REQUIRES_NEW`로 이력 삽입이 별도 트랜잭션에서 실행되어 이력 삽입 실패가 주문 상태 전이를 롤백시키지 않는다.

---

### `application/OrderStatusHistoryApplicationService.java` — 이력 조회 서비스

```java
@Service
public class OrderStatusHistoryApplicationService {
    public List<OrderStatusHistoryResult> getHistory(String orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(OrderStatusHistoryResult::from)
                .toList();
    }
}
```

---

### `application/OrderStatusHistoryResult.java` — 응답 DTO

```java
public record OrderStatusHistoryResult(
        OrderStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime changedAt
) { ... }
```

---

### `OrderController` — 엔드포인트 추가

```java
@GetMapping("/{orderId}/status-history")
public List<OrderStatusHistoryResult> getStatusHistory(@PathVariable String orderId) {
    return orderStatusHistoryApplicationService.getHistory(orderId);
}
```

존재하지 않는 `orderId` 조회 시 빈 배열을 반환한다.

---

### `application/OrderStatusHistoryAspectTest.java` — Aspect 단위 테스트

Spring 컨텍스트 없이 Aspect를 직접 인스턴스화해서 테스트한다. `recordHistory` 메서드에 `OrderResult`를 직접 넘기고, `ArgumentCaptor`로 `orderStatusHistoryRepository.save()`에 전달된 `OrderStatusHistory`의 `orderId`, `status`, `changedAt`을 검증한다. `CREATED`, `CONFIRMED`, `CANCELLED` 세 케이스를 각각 검증한다.

Spring AOP는 프록시 기반이라 포인트컷 동작(어떤 메서드에 advice가 붙는지)은 통합 테스트에서 검증하고, 단위 테스트는 advice 로직 자체(이력이 올바르게 기록되는지)에 집중한다.

---

### `adapter/in/web/OrderStatusHistoryIntegrationTest.java` — 이력 조회 통합 테스트

MockMvc + H2로 실제 흐름을 검증한다.

- 주문 생성 API 호출 후 DB에 이력이 실제로 쌓이는지
- `GET /status-history` 엔드포인트가 올바른 응답(`status`, `changedAt`)을 반환하는지
- 존재하지 않는 `orderId` 조회 시 빈 배열이 반환되는지

각 테스트는 `@BeforeEach`에서 `orderStatusHistoryRepository.deleteAll()`로 이전 테스트의 이력을 초기화한다.

---

## 오류 발생 및 해결 과정

### 오류 1 — `spring-boot-starter-aop` 의존성 해석 실패

**증상**: `Could not find org.springframework.boot:spring-boot-starter-aop:.`

**원인**: Spring Boot 4.0에서 `spring-boot-starter-aop`가 `spring-boot-starter-aspectj`로 이름이 변경됐다. BOM에 기존 이름이 포함되지 않아 버전을 찾지 못했다.

**해결**: `spring-boot-starter-aspectj`로 교체했다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-aspectj'
```

---

### 오류 2 — Jackson `write-dates-as-timestamps` 설정 오류

**증상**: `No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps` — 컨텍스트 로드 실패로 통합 테스트 전체 실패.

**원인**: Spring Boot 4.x는 Jackson 라이브러리가 `com.fasterxml.jackson` → `tools.jackson`으로 변경됐다. `application.yml`의 `spring.jackson.serialization.write-dates-as-timestamps` 속성 키를 enum 상수로 변환하는 방식이 바뀌어 인식하지 못했다.

**해결**: `application.yml`에서 해당 설정을 제거하고, `OrderStatusHistoryResult` DTO의 `changedAt` 필드에 `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")`를 직접 추가해 ISO 8601 형식으로 직렬화했다.

---

## 테스트 게이트 확인

```bash
./gradlew :order-service:test
./gradlew test
```

| 테스트 | 결과 |
|---|---|
| `OrderStatusHistoryAspectTest` — CREATED/CONFIRMED/CANCELLED 이력 저장 검증 | 통과 |
| `OrderStatusHistoryIntegrationTest` — 주문 생성 후 이력 1건 확인 | 통과 |
| `OrderStatusHistoryIntegrationTest` — GET /status-history CREATED 이력 반환 | 통과 |
| `OrderStatusHistoryIntegrationTest` — 존재하지 않는 orderId → 빈 배열 | 통과 |
| 기존 전체 테스트 | 통과 |

---

## 머지 조건 확인

- [x] `OrderStatusHistory` 엔티티/리포지토리 추가 (`orderId` 인덱스 포함)
- [x] AOP Aspect로 `createOrder`, `confirmOrder`, `cancelOrder` 호출 시 이력 삽입 (`REQUIRES_NEW`)
- [x] `GET /orders/{id}/status-history` 엔드포인트 — 시간순 이력 반환, 없으면 빈 배열
- [x] Swagger `@Operation`, `@ApiResponse` 추가
- [x] 단위 테스트 (`OrderStatusHistoryAspectTest`) 통과
- [x] 통합 테스트 (`OrderStatusHistoryIntegrationTest`) 통과
- [x] 기존 전체 테스트 통과

---

## 다음 단계

Task 2 — 프론트엔드: 주문 ID 입력 → 상태 이력 타임라인 렌더링 + 새로고침 버튼 + 에러 처리
