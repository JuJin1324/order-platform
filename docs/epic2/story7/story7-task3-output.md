# Story 7 Task 3 산출물 — Tailwind CSS 적용

---

## 완료한 작업

### Tailwind CSS v4 설치 및 설정

```bash
npm install tailwindcss @tailwindcss/vite
```

**`vite.config.ts`** — Tailwind 플러그인 추가

```ts
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  ...
})
```

**`src/index.css`** — 기존 Vite 기본 CSS를 제거하고 Tailwind import로 대체

```css
@import "tailwindcss";
```

Tailwind v4는 별도 설정 파일(`tailwind.config.js`) 없이 CSS import만으로 동작한다. v3과의 주요 차이점이다.

---

### `OrderCreatePage.tsx` 스타일 적용

- 카드 레이아웃 (`bg-white rounded-2xl shadow-md`)
- 입력 필드 — 포커스 시 파란색 링 표시 (`focus:ring-2 focus:ring-blue-500`)
- 제출 버튼 — 로딩 중 비활성화 시 색상 변화 (`disabled:bg-blue-300`)
- 에러 메시지 — 빨간색 배경 박스 (`bg-red-50 border border-red-200`)

---

### `OrderStatusPage.tsx` 스타일 적용

- 조회 폼 — 입력 필드와 버튼을 가로로 배치 (`flex gap-2`)
- 상태 이력 리스트 — 각 항목을 카드로 표시
- 상태 배지 — 상태별 색상 구분
  - `CREATED` → 노란색 (`bg-yellow-100 text-yellow-800`)
  - `CONFIRMED` → 초록색 (`bg-green-100 text-green-800`)
  - `CANCELLED` → 빨간색 (`bg-red-100 text-red-800`)
- 새로고침 — 버튼 대신 텍스트 링크 스타일 (`text-blue-600 hover:text-blue-800`)

---

## 테스트 게이트 확인

- [x] `tsc --noEmit` 통과
- [x] 주문 생성 폼 UI 정상 렌더링
- [x] 상태 조회 페이지 UI 정상 렌더링
- [x] CREATED / CONFIRMED / CANCELLED 상태 배지 색상 구분 확인
- [x] 기존 기능 동작 이상 없음

---

## 머지 조건 확인

- [x] Tailwind CSS v4 설치 및 설정 완료
- [x] 두 페이지 스타일 적용 완료
- [x] 포트폴리오 첨부 가능한 수준으로 렌더링됨

---

## 다음 단계

Task 4 — order-web Dockerfile + nginx 설정 + docker-compose 추가
