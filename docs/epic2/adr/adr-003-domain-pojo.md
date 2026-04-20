# ADR-003: 도메인 레이어는 외부 프레임워크 어노테이션을 갖지 않는다

## 상태

채택 (Accepted)

## 맥락

Story 2 Task 2에서 Swagger 문서화를 위해 `@Schema` 어노테이션을 추가하는 작업을 진행했다.
`OrderResult`(application 레이어)와 `CreateOrderRequest`(presentation 레이어)에는 `@Schema`를 붙였으나,
`OrderStatus`(domain 레이어)에 동일하게 적용했다가 제거했다.

도메인 레이어에 Swagger, JPA, Jackson 같은 외부 프레임워크 어노테이션이 붙기 시작하면
도메인 모델이 특정 기술 스택에 종속된다. 이는 도메인 로직을 독립적으로 테스트하거나
인프라를 교체할 때 불필요한 마찰을 만든다.

## 결정

`domain/` 패키지의 클래스와 enum은 순수 POJO로 유지한다.
다음 어노테이션은 도메인 레이어에 추가하지 않는다.

- Swagger / OpenAPI: `@Schema`, `@ArraySchema` 등
- JPA: `@Entity`, `@Column` 등 — 단, 현재 `Order` 엔티티는 JPA를 직접 사용하므로 잠정 예외.
  비즈니스 로직이 복잡해지는 시점에 JPA 엔티티를 infrastructure 레이어로 분리하고
  domain 객체와 분리하는 것을 고려한다.
- Jackson: `@JsonProperty`, `@JsonIgnore` 등
- Bean Validation: `@NotNull`, `@Size` 등 (presentation DTO에서 처리)

## 결과

- `OrderStatus`, `Order`(JPA 예외 적용), 향후 추가되는 도메인 객체는 프레임워크 어노테이션 없이 작성한다.
- Swagger 문서화가 필요한 타입은 presentation 레이어의 DTO 또는 application 레이어의 Result 객체에 어노테이션을 추가한다.
- 도메인 테스트는 Spring Context 없이 순수 단위 테스트로 작성할 수 있다.
