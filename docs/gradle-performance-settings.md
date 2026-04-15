# Gradle 성능 설정 가이드

이 프로젝트는 `gradle.properties`에 빌드·테스트 속도를 높이기 위한 설정을 고정해 두었다.
이 문서는 각 설정의 의미와 IntelliJ IDEA에서의 적용 여부를 정리한다.

---

## 현재 설정 (`gradle.properties`)

```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true
org.gradle.vfs.watch=true
org.gradle.workers.max=10
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8
```

### 각 설정의 역할

| 설정 | 역할 |
|---|---|
| `org.gradle.daemon=true` | Gradle 데몬을 유지해 JVM 재시작 비용 제거. 두 번째 빌드부터 빠름. |
| `org.gradle.parallel=true` | 서브 프로젝트를 병렬로 빌드. 멀티 모듈 구조에서 효과적. |
| `org.gradle.caching=true` | 이전 빌드 결과를 캐시. 입력이 같으면 다시 컴파일하지 않음. |
| `org.gradle.configuration-cache=true` | 태스크 그래프 계산 결과를 캐시. 설정 단계 비용 절감. |
| `org.gradle.configuration-cache.parallel=true` | 설정 캐시 직렬화/역직렬화를 병렬 처리. |
| `org.gradle.vfs.watch=true` | 파일 시스템 변경 감지로 증분 빌드 정확도 향상. |
| `org.gradle.workers.max=10` | Gradle이 사용할 최대 워커 스레드 수. |
| `org.gradle.jvmargs` | Gradle 데몬 JVM 옵션. `-Xmx8g`로 힙 확보, `UseParallelGC`로 GC 최적화. |

> **주의**: `org.gradle.jvmargs`는 Gradle 데몬 JVM에 적용된다. 테스트 프로세스 JVM은 별도이며,
> `build.gradle`의 `test { jvmArgs ... }` 블록으로 설정한다.

---

## IntelliJ IDEA에서의 적용 여부

### 조건: Gradle에 위임(Delegate)했는가

`gradle.properties`의 모든 설정은 **IntelliJ가 빌드/테스트를 Gradle에 위임한 경우에만 적용**된다.
IntelliJ 자체 빌더를 사용하면 이 파일은 완전히 무시된다.

| IntelliJ 빌드 방식 | gradle.properties 적용 여부 |
|---|---|
| Build and run using: **Gradle** | **적용됨** |
| Build and run using: **IntelliJ IDEA** | 적용되지 않음 |

### Gradle 위임 설정 방법

```
Settings (⌘,) → Build, Execution, Deployment
  → Build Tools → Gradle
    → Build and run using: Gradle
    → Run tests using: Gradle
```

이 두 항목을 모두 `Gradle`로 설정해야 한다.

### 설정 후 달라지는 점

- 터미널에서 `./gradlew test`를 실행하는 것과 IntelliJ 테스트 버튼을 누르는 것이 **동일한 Gradle 프로세스**를 사용한다.
- 병렬 빌드, 캐시, 데몬 등 모든 성능 설정이 IntelliJ 내에서도 동작한다.
- 터미널에서 빌드한 캐시가 IntelliJ에서도 재사용된다 (역방향도 동일).

---

## IntelliJ 자체 설정 (gradle.properties와 별개)

IntelliJ 자체 JVM 메모리는 `gradle.properties`와 무관하게 별도로 관리된다.
IDE가 느리거나 OOM이 발생하면 아래에서 조정한다.

```
Help → Edit Custom VM Options
```

```
-Xmx4g
-XX:MaxMetaspaceSize=512m
```

이 설정은 IntelliJ 프로세스 자체에 적용되며, Gradle 데몬이나 테스트 JVM과는 무관하다.
