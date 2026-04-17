# Step 3 React 프로젝트 생성 PR 계획

이 문서는 "백엔드만 있던 저장소에 프론트엔드 개발 환경을 어떻게 추가할 것인가"를 정리한다.

---

## 1. 왜 두 PR로 나누는가

Step 3의 작업은 성격이 다른 두 변경으로 구성된다.

- **PR 1**: 프로젝트 생성 — `frontend/` 디렉터리와 Vite 기본 파일 추가
- **PR 2**: 백엔드 연결 — Vite 프록시로 order-service API 호출 가능하게 설정

PR 1은 "React 앱이 뜬다"로 검증되고, PR 2는 "React에서 백엔드와 통신된다"로 검증된다. 두 검증 지점이 분리되어야 각 PR의 머지 기준이 명확하다. 한 PR로 합치면 문제가 발생했을 때 "프로젝트 문제인가, 프록시 문제인가"를 분리하기 어렵다.

---

## 2. 고정 결정

- 빌드 도구: Vite
- 템플릿: `react-ts` (Vite 공식 React + TypeScript 템플릿)
- 위치: 저장소 루트 `frontend/` 디렉터리
- 패키지 매니저: npm (Vite 기본값)
- Gradle 멀티모듈로 통합하지 않는다. `frontend/`는 독립 npm 프로젝트로 유지한다.
- 프록시: `/api/*` 경로를 `http://localhost:8080`(order-service)로 전달

---

## 3. 권장 PR 순서

### PR 1. Vite + React + TypeScript 프로젝트 생성

#### 목표

저장소 루트에 `frontend/` 디렉터리를 만들고, `npm run dev`로 기본 React 화면이 브라우저에서 뜨는 상태를 만든다.

#### 핵심 작업

- `npm create vite@latest frontend -- --template react-ts`로 Vite 프로젝트 생성
- `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json` 등 기본 설정 커밋
- 저장소 루트 `.gitignore`에 `frontend/node_modules/`, `frontend/dist/` 추가
- 루트 `README.md`에 `frontend` 실행 방법 한두 줄 추가

#### 이 PR에서 하지 않을 것

- Vite 프록시 설정 (PR 2)
- 3계층 폴더 구조 (Step 4)
- 정식 API 호출 (Step 5)

#### 테스트 게이트

- `cd frontend && npm install && npm run dev` 실행 시 포트 5173에서 React 기본 화면 렌더링
- 기존 Gradle 빌드·테스트 영향 없음

#### 머지 조건

`http://localhost:5173` 접속 시 Vite 기본 React 화면이 렌더링된다. 백엔드 연결 여부는 이 PR의 검증 대상이 아니다.

---

### PR 2. Vite 프록시 설정으로 백엔드 연결

#### 목표

React 앱의 `/api/*` 요청이 order-service(`:8080`)로 전달되도록 프록시를 설정한다.

#### 핵심 작업

- `frontend/vite.config.ts`에 `server.proxy` 설정 추가
  - `/api` → `http://localhost:8080`
  - `changeOrigin: true`
- 연결 확인용 임시 fetch 코드를 `App.tsx`에 추가하고, 브라우저 Network 탭에서 프록시 동작 확인
- 임시 fetch 코드는 Step 5에서 정식 API 호출로 교체 예정임을 주석 없이 자연스럽게 둔다 (별도 브랜치에서 걷어냄)

#### 이 PR에서 하지 않을 것

- order-service 측 CORS 설정 변경 — 프록시 우회로 해결하므로 불필요
- 정식 API 클라이언트 모듈 (Step 4)
- 실제 폼 UI (Step 5)

#### 테스트 게이트

- 브라우저 DevTools Network 탭에서 `/api/*` 요청이 `localhost:8080`으로 프록시되는 것 확인
- order-service 측 로그에서 요청 수신 확인

#### 머지 조건

React 앱에서 보낸 `/api/*` 요청에 order-service가 응답을 돌려준다.

---

## 4. 이 계획에서 의도적으로 뒤로 미룬 것

- 3계층 폴더 구조(`pages/`, `hooks/`, `api/`, `types/`) — Step 4
- Custom Hook, React Router — Step 4
- 실제 API 호출, 폼 UI — Step 5
- 프로덕션 빌드 설정, 환경 변수 분리(`.env.development` 등) — Phase 2 범위 밖

---

## 5. 권장 순서 요약

1. Vite 프로젝트 생성
2. React 기본 화면 렌더링 확인
3. Vite 프록시 설정
4. `/api/*` 프록시 동작 확인
