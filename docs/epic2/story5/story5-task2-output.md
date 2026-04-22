# Story 5 Task 2 산출물 — 에러 처리 + UX 개선

---

## 완료한 작업

### `order-web/src/pages/OrderCreatePage.tsx` — 에러 메시지 표시

Hook에서 넘어온 `error`는 HTTP status code 문자열(`"400"`, `"500"`) 또는 네트워크 오류 메시지다. 페이지에서 이를 사용자 친화적인 메시지로 변환한다.

```tsx
function toErrorMessage(error: string): string {
  if (error === '400') return '입력값이 올바르지 않습니다. (수량은 1 이상, 금액은 1원 이상)';
  if (error === '500') return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  return '네트워크 오류가 발생했습니다. 연결 상태를 확인해주세요.';
}
```

Hook은 원시 에러(status code)만 노출하고, "어떤 메시지를 보여줄지"는 페이지가 결정한다. 이 역할 분리가 3계층 설계 포인트다.

재제출 시 이전 에러 초기화는 `useOrderCreate` Hook의 `submitOrder` 함수 시작 시점에 `setError(null)`을 호출하는 방식으로 이미 구현되어 있었다.

### 폼 기본값 설정

매번 값을 입력하지 않아도 되도록 개발 편의를 위해 기본값을 설정했다.

```tsx
const [sku, setSku] = useState('ITEM-001');
const [quantity, setQuantity] = useState('2');
const [amount, setAmount] = useState('29900');
```

### 시도했다가 제거한 것 — 성공 후 폼 필드 초기화

처음에는 성공 후 폼 필드를 초기화하는 `useEffect`를 추가했다.

```tsx
useEffect(() => {
  if (result) {
    setSku('');
    setQuantity('');
    setAmount('');
  }
}, [result]);
```

그런데 테스트할 때 값을 다시 입력해야 해서 불편했다. 테스트 편의성을 위해 제거했다. 성공 후 폼 필드 초기화는 Story 7에서 상태 조회 페이지로 자동 이동하는 흐름이 붙으면 자연스럽게 해결된다.

---

## 테스트 게이트 확인

| 시나리오 | 방법 | 결과 |
|---|---|---|
| 정상 제출 | `ITEM-001`, 수량 `2`, 금액 `29900` | 주문 ID + CREATED 상태 표시 |
| 400 오류 | 음수 수량(`-1`) 입력 후 제출 | "입력값이 올바르지 않습니다." 표시 |
| 네트워크 오류 | DevTools Network 탭 → Offline 설정 후 제출 | "네트워크 오류가 발생했습니다." 표시 |
| 재제출 | 에러 후 정상값으로 다시 제출 | 이전 에러 메시지 사라지고 정상 처리 |

---

## 머지 조건 확인

- [x] 400 응답 시 입력값 오류 메시지 표시
- [x] 네트워크 오류 시 연결 오류 메시지 표시
- [x] 재제출 시 이전 에러 초기화
- [x] 로딩 중 버튼 중복 클릭 방지 (`disabled`)
- [x] `tsc --noEmit` 통과

---

## 다음 단계

Story 6 — 주문 상태 이력 조회 화면 (백엔드 `OrderStatusHistory` + 프론트엔드 타임라인)
