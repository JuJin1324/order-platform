# Story 7 Task 4 산출물 — order-web Docker 컨테이너화

---

## 완료한 작업

### `order-web/Dockerfile` — 멀티 스테이지 빌드

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

1단계(build): Node.js 22 Alpine에서 `npm run build`로 정적 파일 생성 (`dist/`)
2단계(serve): nginx Alpine에 `dist/`를 복사해 서빙. 최종 이미지에 Node.js가 포함되지 않아 이미지 크기가 작다.

---

### `order-web/nginx.conf` — SPA 라우팅 + API 프록시

```nginx
server {
    listen 80;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://order-service:8081;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

`try_files $uri $uri/ /index.html` — SPA 라우팅 핵심 설정이다. `/orders/abc123/status` 같은 경로로 직접 접근해도 파일이 없으면 `index.html`을 반환해 React Router가 처리하게 한다. 이 설정이 없으면 새로고침 시 nginx가 404를 반환한다.

`proxy_pass http://order-service:8081` — `/api/` 요청을 Docker 내부 네트워크의 order-service로 전달한다. Vite 개발 서버의 프록시 역할을 nginx가 대신한다.

---

### `docker-compose.yml` — order-web 서비스 추가

```yaml
order-web:
  build:
    context: ./order-web
  ports:
    - "5173:80"
  depends_on:
    - order-service
```

포트 `5173:80` — 호스트 5173을 컨테이너 80(nginx)에 매핑한다. Vite 개발 서버와 동일한 포트를 사용해 전환이 자연스럽다. 단, Vite 개발 서버와 동시에 실행하면 포트가 충돌할 수 있다.

---

## 오류 발생 및 해결 과정

### 오류 1 — Docker 빌드 시 TypeScript 타입 import 오류

**증상**: `npm run build` 실패. `'FormEvent' is a type and must be imported using a type-only import when 'verbatimModuleSyntax' is enabled.`

**원인**: Vite 개발 서버(`npm run dev`)는 esbuild로 트랜스파일하면서 타입 오류를 느슨하게 처리하지만, `npm run build`는 `tsc -b`로 엄격한 타입 검사를 먼저 실행한다. `FormEvent`를 일반 import로 가져온 것이 빌드 단계에서 오류로 잡혔다.

**해결**: `import type { FormEvent }` 로 수정.

### 오류 2 — IPv6/IPv4 포트 충돌

**증상**: `curl http://localhost:5173` 이 404를 반환했다.

**원인**: Vite 개발 서버가 IPv6(`::1:5173`)에서 실행 중이었고, Docker는 IPv4(`0.0.0.0:5173`)에 바인딩됐다. curl이 IPv6를 먼저 시도해 Vite 개발 서버에 연결됐다.

**해결**: `curl http://127.0.0.1:5173`(IPv4 명시)으로 확인 → 200 응답. Vite 개발 서버를 종료하면 브라우저에서 `localhost:5173`으로 컨테이너에 정상 접속된다.

---

## 테스트 게이트 확인

```bash
make up
curl http://127.0.0.1:5173
```

→ HTTP 200, nginx가 order-web 정적 파일 서빙 확인

---

## 머지 조건 확인

- [x] `order-web/Dockerfile` 멀티 스테이지 빌드
- [x] `order-web/nginx.conf` SPA 라우팅 + API 프록시
- [x] `docker-compose.yml`에 order-web 서비스 추가
- [x] `make up` 후 `http://127.0.0.1:5173` 200 응답 확인

---

## 다음 단계

Story 8 — 포트폴리오 작성
