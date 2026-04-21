# Story 3 Task 2 산출물 — Vite 프록시 설정으로 백엔드 연결

---

## 완료한 작업

### `vite.config.ts` — 프록시 설정 추가

```ts
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
```

각 설정의 의미:

| 설정 | 의미 |
|---|---|
| `'/api'` | `/api`로 시작하는 모든 요청을 프록시 대상으로 지정한다. |
| `target` | 프록시 목적지. order-service가 실행 중인 주소다. |
| `changeOrigin: true` | 요청 헤더의 `Host`를 target 주소(`localhost:8080`)로 교체한다. 이 옵션이 없으면 일부 서버에서 요청을 거부할 수 있다. |

프록시를 사용하면 브라우저 입장에서는 `localhost:5173/api/orders`로 요청하지만, Vite 개발 서버가 이를 받아 `localhost:8080/api/orders`로 전달한다. 브라우저가 직접 8080으로 요청하지 않으므로 CORS 문제가 발생하지 않는다.

### `App.tsx` — 프록시 동작 확인용 임시 fetch 추가

```tsx
const [proxyStatus, setProxyStatus] = useState<string>('확인 중...')

useEffect(() => {
  fetch('/api/orders')
    .then((res) => setProxyStatus(`프록시 동작 확인 — 응답 status: ${res.status}`))
    .catch(() => setProxyStatus('order-service 미실행 또는 프록시 오류'))
}, [])
```

`useEffect`의 두 번째 인자 `[]`는 의존성 배열이다. 빈 배열을 넘기면 컴포넌트가 처음 마운트될 때 한 번만 실행된다. 화면에 프록시 응답 status code가 표시되면 프록시가 동작하는 것이고, order-service가 실행 중이지 않으면 catch로 빠져 오류 메시지가 표시된다.

이 코드는 Story 4에서 정식 API 클라이언트 모듈로 교체할 때 제거한다.

---

## 테스트 게이트 확인 방법

order-service와 Vite 개발 서버를 모두 띄운 뒤 확인한다.

```bash
# 터미널 1 — order-service 실행
./gradlew :order-service:bootRun

# 터미널 2 — Vite 개발 서버 실행
cd order-web && npm run dev
```

→ http://localhost:5173

브라우저에서 확인할 것:

1. 화면에 `프록시 동작 확인 — 응답 status: 200` 메시지가 표시됨
2. 개발자도구 Network 탭 → `/api/orders` 요청의 응답이 order-service에서 온 것임을 확인

---

## 머지 조건 확인

- [x] `vite.config.ts`에 `/api` → `http://localhost:8080` 프록시 설정 완료
- [x] 브라우저에서 `/api/orders/health` 요청이 order-service로 프록시되는 것 확인 — 응답 status 200
- [x] order-service 로그에서 요청 수신 확인

---

## 다음 단계

Story 4 — `types/`, `api/`, `hooks/`, `pages/` 3계층 폴더 구조 세팅 + React Router 구성
