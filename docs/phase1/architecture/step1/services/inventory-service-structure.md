# inventory-service 기능 구조

이 문서는 `inventory-service`가 무엇을 하는 서비스인지 정리한다.  
근거는 [c4-container-structure.md](../c4-container-structure.md)와 [problem-solving-structure.md](../problem-solving-structure.md)다.

---

## 1. 한 줄 정의

`inventory-service`는 재고 차감의 성공/실패를 결정하는 마지막 단계다.

- `payment-service`로부터 재고 차감 요청을 받는다.
- 재고가 충분하면 차감하고, 부족하면 실패를 반환한다.
- 다른 서비스를 호출하지 않는다. 호출 체인의 끝이다.

---

## 2. 인터페이스

### 받는 요청

| 메서드 | 경로 | 호출자 | 목적 |
|---|---|---|---|
| POST | `/internal/inventory/deduct` | payment-service | 재고 차감 |
| GET | `/api/inventory/{sku}` | Client | 재고 조회 |

### 보내는 요청

없음. 호출 체인의 종단이다.

---

## 3. 재고 상태

재고는 별도 상태 머신이 없다. 수량(`availableQuantity`)이 직접 변한다.

- 차감 성공 → 수량 감소
- 차감 실패 → 수량 변화 없음

실패 조건은 두 가지다:
1. 재고 부족 (`availableQuantity < quantity`)
2. 강제 실패 (`forceFailure = true`) — 일관성 깨짐 재현용

---

## 4. API 스펙

### 4.1 재고 차감

```
POST /internal/inventory/deduct
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| sku | String | O | 상품 식별자 |
| quantity | Integer | O | 차감 수량 (1 이상) |
| forceFailure | boolean | X | `true`면 강제 실패. 일관성 깨짐 재현용. 기본값 `false` |

**Response Body (성공)**

| 필드 | 타입 | 설명 |
|---|---|---|
| sku | String | 상품 식별자 |
| deductedQuantity | Integer | 차감된 수량 |
| remainingQuantity | Integer | 차감 후 남은 수량 |

**Response (실패)**

재고 부족 또는 강제 실패 시 `409 CONFLICT`를 반환한다.

### 4.2 재고 조회

```
GET /api/inventory/{sku}
```

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| sku | String | 조회할 상품 식별자 |

**Response Body**

| 필드 | 타입 | 설명 |
|---|---|---|
| sku | String | 상품 식별자 |
| availableQuantity | Integer | 현재 가용 수량 |

### 4.3 헬스 체크

```
GET /api/inventory/health
```

서비스 생존 확인용. 별도 파라미터 없음.
