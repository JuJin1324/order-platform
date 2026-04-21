# 학습 — fetch vs axios

---

## 비교

| | fetch | axios |
|---|---|---|
| **설치** | 브라우저 내장, 별도 설치 없음 | `npm install axios` 필요 |
| **응답 파싱** | `res.json()` 직접 호출해야 함 | 자동으로 JSON 파싱 |
| **에러 처리** | HTTP 4xx/5xx를 에러로 던지지 않음, 직접 체크 필요 | HTTP 4xx/5xx를 자동으로 에러로 던짐 |
| **요청 인터셉터** | 없음, 직접 래핑해야 함 | 기본 제공 |
| **타임아웃** | 없음, AbortController 직접 구현 | 옵션 한 줄로 설정 |
| **번들 사이즈** | 0kb (내장) | ~14kb |

---

## 에러 처리 차이

fetch는 HTTP 4xx/5xx를 에러로 간주하지 않는다. 네트워크 자체가 끊겼을 때만 `catch`로 빠진다. 응답이 왔다면 status code와 무관하게 `then`으로 간다.

```ts
// fetch — 직접 체크해야 함
const res = await fetch('/api/orders')
if (!res.ok) throw new Error(`${res.status}`)  // 이 줄이 없으면 404, 500도 정상 처리됨
const data = await res.json()
```

```ts
// axios — 4xx/5xx면 자동으로 throw
const { data } = await axios.post('/api/orders', body)
```

---

## order-web의 선택: fetch

axios 없이 fetch를 사용한다. 근거는 의존성 최소화다. Epic 2 규모(API 함수 2~3개)에서는 axios의 편의 기능(인터셉터, 타임아웃)이 필요한 상황이 없다. 대신 `api/orderApi.ts`에서 `res.ok` 체크를 직접 처리한다.

인터셉터나 타임아웃이 실제로 필요해지면 그때 axios 도입을 검토한다.
