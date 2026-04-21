# 학습 — Spring과 React 계층 대응

---

## 계층 대응표

| 역할 | Spring | React |
|---|---|---|
| **진입점 / 요청 수신** | `@RestController` | Page 컴포넌트 (`OrderCreatePage.tsx`) |
| **조율 계층** | `@Service` (Application Service) | Custom Hook (`useOrderCreate.ts`) |
| **외부 통신** | `RestTemplate` / `WebClient` / `Repository` | `api/orderApi.ts` (fetch 래핑) |
| **데이터 모델** | DTO (`CreateOrderRequest`, `OrderResult`) | TypeScript 타입 (`OrderRequest`, `OrderResponse`) |

---

## 차이점

Spring Application Service는 무상태(stateless)다. 요청마다 새로 생성되고 상태를 들고 있지 않는다.

Custom Hook은 UI 상태를 직접 들고 있다. `isLoading`, `result`, `error` 같은 값이 Hook 안에 캡슐화되고, 상태가 바뀌면 컴포넌트가 다시 렌더링된다. "조율 계층"이라는 역할은 같지만, 상태를 소유한다는 점이 다르다.

---

## 흐름 비교

**Spring**
```
HTTP 요청 → Controller → Application Service → Repository → DB
```

**React**
```
사용자 이벤트 → Page 컴포넌트 → Custom Hook → api/ 함수 → 백엔드
```
