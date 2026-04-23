import { useState, FormEvent, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useOrderCreate } from '../hooks/useOrderCreate';

function toErrorMessage(error: string): string {
  if (error === '400') return '입력값이 올바르지 않습니다. (수량은 1 이상, 금액은 1원 이상)';
  if (error === '500') return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  return '네트워크 오류가 발생했습니다. 연결 상태를 확인해주세요.';
}

export default function OrderCreatePage() {
  const { submitOrder, isLoading, result, error } = useOrderCreate();
  const navigate = useNavigate();

  const [sku, setSku] = useState('ITEM-001');
  const [quantity, setQuantity] = useState('2');
  const [amount, setAmount] = useState('29900');

  useEffect(() => {
    if (result) {
      navigate(`/orders/${result.orderId}/status`);
    }
  }, [result]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    submitOrder({
      sku,
      quantity: Number(quantity),
      amount: Number(amount),
    });
  }

  return (
    <div>
      <h1>주문 생성</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label>상품 ID (SKU)</label>
          <input value={sku} onChange={(e) => setSku(e.target.value)} required />
        </div>
        <div>
          <label>수량</label>
          <input type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
        </div>
        <div>
          <label>결제 금액 (원)</label>
          <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </div>
        <button type="submit" disabled={isLoading}>
          {isLoading ? '처리 중...' : '주문 생성'}
        </button>
      </form>

      {result && (
        <div>
          <p>주문 완료</p>
          <p>주문 ID: {result.orderId}</p>
          <p>상태: {result.status}</p>
        </div>
      )}
      {error && <p>{toErrorMessage(error)}</p>}
    </div>
  );
}
