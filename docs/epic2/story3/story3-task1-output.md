# Story 3 Task 1 산출물 — Vite + React + TypeScript 프로젝트 생성

---

## 완료한 작업

### Vite 프로젝트 생성

```bash
npm create vite@latest order-web -- --template react-ts
```

각 인자의 의미:

| 인자 | 의미 |
|---|---|
| `create vite@latest` | npm의 `create` 명령으로 `create-vite` 패키지를 실행한다. `@latest`는 최신 버전을 사용한다는 의미. |
| `order-web` | 생성할 디렉터리 이름. 이 이름이 `package.json`의 `name` 필드에도 들어간다. |
| `--` | npm에게 "이후 인자는 내가 쓰는 게 아니라 실행하는 패키지(`create-vite`)에 넘겨라"라고 알리는 구분자. |
| `--template react-ts` | Vite 공식 템플릿 중 React + TypeScript 조합을 선택한다. |

`create-vite`는 선택한 템플릿에 맞춰 `index.html`, `src/main.tsx`, `src/App.tsx`, `vite.config.ts`, `tsconfig.json` 등 기본 파일 세트를 생성한다.

### `.gitignore` 업데이트

`order-web/node_modules/`, `order-web/dist/` 추가. `node_modules/`는 `npm install`로 로컬에 설치되는 패키지 디렉터리로 저장소에 포함하지 않는다. `dist/`는 `npm run build`로 생성되는 프로덕션 빌드 결과물이다.

### `README.md` 업데이트

루트 README에 프론트엔드 실행 방법(`cd order-web && npm install && npm run dev`) 추가.

### 테스트 게이트 통과

```bash
cd order-web && npm install && npm run dev
```

→ http://localhost:5173

`localhost:5173` HTTP 200 확인, Gradle 빌드 영향 없음 확인.

생성된 주요 파일:

```
order-web/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── eslint.config.js
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── App.css
    └── index.css
```

---

## 학습 — SPA 진입점 흐름 따라가기

### 1. `index.html` — SPA의 시작점

```html
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.tsx"></script>
</body>
```

전통적인 MPA는 페이지마다 HTML 파일이 있다. SPA는 `index.html` 하나만 있고, 브라우저가 이 파일을 한 번 받은 뒤부터는 서버에 새 HTML을 요청하지 않는다. `<div id="root">`가 이후 모든 화면이 마운트되는 컨테이너다.

### 2. `main.tsx` — JS가 DOM을 점령하는 지점

```tsx
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

`createRoot`가 `<div id="root">`를 React 관리 영역으로 등록한다. 이 시점부터 화면 교체는 서버가 아니라 React(JS)가 담당한다. `<App />`이 최초로 렌더링되는 컴포넌트다.

### 3. `App.tsx` — 컴포넌트와 `useState`

```tsx
function App() {
  const [count, setCount] = useState(0)

  return (
    <button onClick={() => setCount((count) => count + 1)}>
      Count is {count}
    </button>
  )
}
```

**컴포넌트**: 함수가 JSX를 반환하는 구조. `App`은 하나의 UI 단위다.

**JSX**: HTML처럼 생겼지만 JS 표현식(`{count}`)을 중괄호로 삽입할 수 있다. 브라우저가 직접 이해하는 게 아니라 Vite가 빌드 시 JS로 변환한다.

**`useState`**: `count`(현재 값)와 `setCount`(값을 바꾸는 함수)를 반환한다. 버튼을 클릭하면 `setCount`가 `count`를 1 증가시키고, React가 컴포넌트를 다시 렌더링해 화면에 반영한다. 서버 요청 없이 DOM만 바뀐다.

### 4. SPA vs MPA 한 줄 요약

> MPA는 페이지 이동마다 서버에서 새 HTML을 받아 전체 화면을 교체하고, SPA는 최초 HTML 하나를 받은 뒤 JS가 DOM을 직접 교체해 화면 전환을 처리한다.

---

## 머지 조건 확인

- [x] `http://localhost:5173` 접속 시 Vite 기본 React 화면 렌더링됨
- [x] Gradle 빌드·테스트 영향 없음
- [x] SPA와 MPA의 차이를 한 문장으로 설명할 수 있는 상태
- [x] React 컴포넌트·JSX·`useState` 개념 파악

---

## 다음 단계

Task 2 — Vite 프록시 설정으로 `/api/*` 요청을 order-service(`:8080`)로 전달
