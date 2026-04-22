# Story 5 Task 0 산출물 — docker compose 전체 스택 구성

---

## 완료한 작업

### Dockerfile — 각 서비스

세 서비스에 동일한 Dockerfile을 추가했다. `./gradlew bootJar`로 빌드된 JAR를 Java 21 JRE 이미지 위에 올리는 구조다.

```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

적용 위치: `order-service/Dockerfile`, `payment-service/Dockerfile`, `inventory-service/Dockerfile`

### `docker-compose.yml` — 전체 스택 구성

기존 Kafka만 있던 파일에 세 서비스를 추가했다. Kafka 이미지는 `bitnami/kafka:3.8`(태그 없음)에서 `apache/kafka:latest`로 교체했다.

```yaml
services:
  kafka:
    image: apache/kafka:latest

  order-service:
    build:
      context: ./order-service
    ports:
      - "8081:8081"
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - kafka

  payment-service:
    build:
      context: ./payment-service
    ports:
      - "8082:8082"
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - kafka

  inventory-service:
    build:
      context: ./inventory-service
    ports:
      - "8083:8083"
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - kafka
```

`KAFKA_BOOTSTRAP_SERVERS: kafka:9092` — 컨테이너 내부에서 Kafka에 접근할 때는 `localhost:9092`가 아니라 서비스 이름(`kafka`)으로 참조한다. 각 서비스의 `application.yml`에 `${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}` 형태로 환경변수를 지원하도록 이미 설정되어 있어서 별도 코드 수정 없이 적용됐다.

### `Makefile` — 자주 쓰는 명령 묶음

```makefile
up:
	./gradlew bootJar
	docker compose up -d --build
	docker compose ps

down:
	docker compose down

ps:
	docker compose ps

logs:
	docker compose logs -f
```

| 명령 | 설명 |
|---|---|
| `make up` | JAR 빌드 → 이미지 빌드 → 전체 스택 기동 → 상태 출력 |
| `make down` | 전체 스택 종료 |
| `make ps` | 컨테이너 상태 확인 |
| `make logs` | 전체 로그 스트리밍 |

`--build` 플래그로 코드 변경 시 이미지를 항상 다시 빌드한다.

---

## 테스트 게이트 확인

```bash
make up
curl http://localhost:8081/api/orders/health
```

→ `{"service":"order-service","status":"ok"}`

---

## 머지 조건 확인

- [x] `docker compose up -d` 후 모든 컨테이너 정상 기동
- [x] `http://localhost:8081/api/orders/health` 200 응답 확인

---

## 다음 단계

Task 1 — 주문 생성 폼 UI 구현 + `POST /api/orders` 실제 호출
