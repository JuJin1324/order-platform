import { useState, FormEvent } from 'react';
import { useOrderCreate } from '../hooks/useOrderCreate';

export default function OrderCreatePage() {
  const { submitOrder, isLoading, result, error } = useOrderCreate();

  const [sku, setSku] = useState('');
  const [quantity, setQuantity] = useState('');
  const [amount, setAmount] = useState('');

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
      {error && <p>오류: {error}</p>}
    </div>
  );
}
