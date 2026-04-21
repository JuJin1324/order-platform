# order-web

order-platform의 프론트엔드 모듈. Vite + React + TypeScript로 구성된 SPA다.

---

## 실행

```bash
npm install
npm run dev
```

→ http://localhost:5173

order-service(`http://localhost:8080`)가 함께 실행 중이어야 API 호출이 동작한다.

---

## 주요 명령

| 명령 | 설명 |
|---|---|
| `npm run dev` | Vite 개발 서버 실행 (포트 5173, HMR 활성화) |
| `npm run build` | 프로덕션 빌드 (`dist/` 생성) |
| `npm run lint` | ESLint 검사 |

---

## 프록시

개발 서버는 `/api/*` 요청을 `http://localhost:8080`으로 프록시한다. CORS 설정 없이 백엔드와 통신 가능하다. (`vite.config.ts` 참고)
