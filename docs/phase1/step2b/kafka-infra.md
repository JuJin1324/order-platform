# Kafka 인프라 설정 — Step 2b

## 1. 개요

이 문서는 Kafka 브로커 자체의 구성을 다룬다.
로컬 개발 환경에서 브로커를 띄우는 방법과, 운영 환경에서 반드시 검토해야 할 브로커 수준의 설정 항목을 정리한다.
애플리케이션(Spring) 설정은 `kafka-application.md`를 참고한다.

---

## 2. 로컬 개발 환경 — docker-compose.yml

로컬 개발 환경에서는 `docker-compose.yml`로 Kafka를 단일 컨테이너로 띄운다.

```bash
docker-compose up -d
```

```yaml
services:
  kafka:
    image: bitnami/kafka:3.8
    container_name: order-saga-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 0@kafka:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CFG_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_KRAFT_CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
      ALLOW_PLAINTEXT_LISTENER: "yes"
```

| 항목 | 값 | 의도 |
|---|---|---|
| `image` | `bitnami/kafka:3.8` | Zookeeper 없이 단독 실행 가능한 이미지다. 로컬 개발 목적이므로 클러스터 구성 없이 브로커 한 대로 충분하다. |
| `PROCESS_ROLES: controller,broker` | 하나의 프로세스가 controller와 broker 역할을 동시에 수행 | Kafka 3.x의 KRaft 모드를 사용하면 Zookeeper가 필요 없다. 로컬에서 별도 Zookeeper 컨테이너를 띄우지 않아도 된다. |
| `CONTROLLER_QUORUM_VOTERS` | `0@kafka:9093` | KRaft 클러스터에서 투표에 참여할 controller 목록이다. 단일 노드이므로 자기 자신(node ID 0)만 등록한다. |
| `LISTENERS` | `PLAINTEXT://:9092, CONTROLLER://:9093` | 외부(애플리케이션)와 통신하는 PLAINTEXT 리스너는 9092, controller 간 내부 통신은 9093으로 분리한다. |
| `ADVERTISED_LISTENERS` | `PLAINTEXT://localhost:9092` | 클라이언트가 실제로 접속할 주소다. 컨테이너 내부 hostname(`kafka`) 대신 `localhost`로 광고해야 호스트 머신의 애플리케이션이 접속할 수 있다. |
| `AUTO_CREATE_TOPICS_ENABLE: "true"` | 토픽 자동 생성 허용 | 로컬 개발 편의를 위해 활성화한다. 애플리케이션이 존재하지 않는 토픽에 메시지를 발행하면 Kafka가 자동으로 토픽을 만들어 준다. |
| `ALLOW_PLAINTEXT_LISTENER: "yes"` | 평문(암호화 없음) 리스너 허용 | 로컬 환경이므로 TLS 설정 없이 단순하게 연결한다. |

> **시나리오 테스트와의 관계**: `KafkaOrderProcessingScenarioTest`는 docker-compose가 아닌 Testcontainers(`apache/kafka-native:3.8.0`)로 Kafka를 직접 띄운다. 따라서 시나리오 테스트는 docker-compose 없이도 독립적으로 실행된다. docker-compose는 로컬에서 서비스를 `bootRun`으로 직접 구동할 때 사용한다.

---

## 3. 운영 환경에서 추가로 설정해야 할 항목

현재 docker-compose 설정은 로컬 개발 편의에 맞춰져 있다. 운영 환경에서는 아래 항목들을 반드시 검토해야 한다.

### 브로커 클러스터 구성 (고가용성)

현재는 브로커 1대(단일 노드)로 구성되어 있다. 브로커가 1대이면 장애 시 Kafka 전체가 중단되고, 데이터 복제도 불가능하다. 운영 환경에서는 최소 3대의 브로커를 구성해야 한다.

```yaml
# 브로커를 3대로 분리하고, 각각 node ID / listeners / advertised listeners를 다르게 설정
KAFKA_CFG_NODE_ID: 1   # 브로커마다 고유한 ID
KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@broker1:9093,2@broker2:9093,3@broker3:9093
```

### 토픽 자동 생성 비활성화

```yaml
KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "false"
```

`true`로 두면 오타가 난 토픽 이름으로도 토픽이 생성된다. 운영에서는 토픽을 명시적으로 생성하고, 파티션 수와 복제 인수를 의도에 맞게 직접 지정해야 한다.

### 토픽 복제 인수 및 최소 동기화 복제본 (`min.insync.replicas`)

토픽 생성 시 복제 인수(replication factor)와 `min.insync.replicas`를 함께 설정해야 메시지 유실을 방지할 수 있다.

```bash
# 토픽 생성 예시 (브로커 3대 기준)
kafka-topics.sh --create \
  --topic order-created \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2
```

- `replication-factor 3`: 메시지를 3개 브로커에 복제하여 2대가 동시에 죽어도 데이터가 보존된다.
- `min.insync.replicas=2`: 최소 2개 복제본에 기록이 확인되어야 발행 성공으로 인정한다. 1개 브로커가 다운된 상태에서도 발행이 가능하면서, 메시지 유실을 차단한다.

> **애플리케이션과의 짝 설정**: `min.insync.replicas`는 브로커/토픽 수준의 설정이고, 이것이 실제로 효력을 발휘하려면 애플리케이션 Producer에서 `acks=all`을 함께 설정해야 한다. `acks=all`이 없으면 브로커가 복제 확인을 요구하더라도 Producer가 리더 응답만 받고 성공으로 처리하기 때문이다. 애플리케이션 설정은 `kafka-application.md`를 참고한다.

### 보안 — SASL 인증 (+ TLS는 상황에 따라)

**SASL(Simple Authentication and Security Layer)** 은 네트워크 프로토콜에 인증 기능을 추가하기 위한 프레임워크다. 어떤 인증 방식을 쓸지는 SASL 위에서 선택하는 메커니즘에 따라 달라진다. Kafka에서 주로 쓰이는 메커니즘은 두 가지다.

| 메커니즘 | 특징 |
|---|---|
| `SCRAM-SHA-256` | username/password 기반. 브로커에 계정을 등록하고 클라이언트가 자격증명으로 접속한다. 설정이 단순하여 일반적인 운영 환경에 적합하다. |
| `OAUTHBEARER` | OAuth 2.0 토큰 기반. 외부 Identity Provider(Keycloak 등)와 연동할 때 사용한다. |

요약하면, **SASL은 "누가 접속하는가"를 검증하는 인증 레이어**이고, TLS는 "전송 중 데이터를 암호화"하는 레이어다. 둘은 독립적으로 적용할 수 있다.

현재는 `ALLOW_PLAINTEXT_LISTENER: "yes"`로 암호화와 인증이 모두 비활성화되어 있다. 운영 환경에서는 최소한 SASL 인증을 추가해야 한다.

**TLS 암호화는 네트워크 경계와 규정에 따라 판단한다.**
Kafka는 일반적으로 사설 네트워크(VPC, 온프레미스 내부망) 안에 위치하며, 네트워크 수준의 접근 제어(Security Group, 방화벽)가 이미 적용된 환경이 대부분이다.
이런 경우 TLS 없이 SASL 인증만 두는 것도 현실적인 선택이다. 고처리량 환경에서 TLS 암호화/복호화 비용이 무시할 수 없기 때문이다.

반면 아래 상황에서는 TLS를 함께 적용해야 한다.
- 멀티 리전·멀티 클라우드 등 트래픽이 공용 네트워크를 경유하는 경우
- 금융·의료처럼 전송 중 암호화를 법적으로 요구하는 규정이 있는 경우
- Zero Trust 보안 정책을 따르는 경우

#### SASL만 적용하는 경우 (내부망, TLS 생략)

```yaml
# 브로커 환경변수
KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: SASL_PLAINTEXT:SASL_PLAINTEXT,CONTROLLER:PLAINTEXT
KAFKA_CFG_INTER_BROKER_LISTENER_NAME: SASL_PLAINTEXT
KAFKA_CFG_SASL_MECHANISM_INTER_BROKER_PROTOCOL: SCRAM-SHA-256
```

#### TLS + SASL을 함께 적용하는 경우

```yaml
# 브로커 환경변수
KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: SASL_SSL:SASL_SSL,CONTROLLER:PLAINTEXT
KAFKA_CFG_INTER_BROKER_LISTENER_NAME: SASL_SSL
KAFKA_CFG_SASL_MECHANISM_INTER_BROKER_PROTOCOL: SCRAM-SHA-256
```

> **애플리케이션과의 짝 설정**: 브로커의 보안 프로토콜을 변경하면 클라이언트(애플리케이션)도 동일한 프로토콜로 접속해야 한다. `application.yml`의 `security.protocol`과 `sasl.jaas.config`를 함께 변경해야 한다. 애플리케이션 설정은 `kafka-application.md`를 참고한다.
