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

**React 배포의 핵심 개념 — 소스코드 vs 배포 파일**

React 코드(`.tsx`, `.ts`)는 브라우저가 직접 읽지 못한다. Vite가 이 코드를 브라우저가 읽을 수 있는 JavaScript/HTML/CSS로 변환해줘야 한다. 이 변환 과정이 `npm run build`이고, 결과물이 `dist/` 폴더에 생긴다. 배포할 때 필요한 건 `dist/` 안의 정적 파일뿐이다.

**왜 nginx인가**

`dist/` 안의 정적 파일은 Node.js 없이 어떤 웹 서버로도 서빙할 수 있다. nginx는 정적 파일 서빙에 특화된 가볍고 빠른 웹 서버다. Vite 개발 서버는 개발 편의 기능(HMR, 프록시 등)을 위해 Node.js가 필요하지만, 배포 환경에서는 그런 기능이 불필요하다.

**왜 멀티 스테이지 빌드인가**

빌드(1단계)와 서빙(2단계)을 분리하는 이유는 최종 이미지 크기 때문이다. Node.js는 `npm run build`를 실행할 때만 필요하다. 빌드가 끝나면 Node.js, `node_modules`, 소스코드는 전부 필요 없고 `dist/`만 있으면 된다. 1단계에서 빌드하고 `dist/`만 2단계(nginx)로 넘기면, 최종 이미지에 Node.js가 포함되지 않아 이미지가 훨씬 가벼워진다.

**흐름 요약**

소스코드 → `npm run build`(Node.js) → `dist/` → nginx가 브라우저에 서빙

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
    - "80:80"
  depends_on:
    - order-service
```

포트 `80:80` — 호스트 80을 컨테이너 80(nginx)에 매핑한다. Vite 개발 서버(5173)와 포트가 분리되어 동시에 실행해도 충돌하지 않는다. 브라우저에서 `http://localhost`로 접속한다.

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
curl http://localhost:80
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
