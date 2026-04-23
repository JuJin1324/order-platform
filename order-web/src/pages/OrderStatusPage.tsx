import { useState, FormEvent } from 'react';
import { useOrderStatus } from '../hooks/useOrderStatus';

function toErrorMessage(error: string): string {
  return '네트워크 오류가 발생했습니다. 연결 상태를 확인해주세요.';
}

export default function OrderStatusPage() {
  const { fetchHistory, refresh, isLoading, history, error } = useOrderStatus();
  const [orderId, setOrderId] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    fetchHistory(orderId);
  }

  return (
    <div>
      <h1>주문 상태 조회</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label>주문 ID</label>
          <input value={orderId} onChange={(e) => setOrderId(e.target.value)} required />
        </div>
        <button type="submit" disabled={isLoading}>조회</button>
      </form>

      {history.length > 0 && (
        <div>
          <button onClick={refresh} disabled={isLoading}>새로고침</button>
          <ul>
            {history.map((item, index) => (
              <li key={index}>
                {item.status} — {item.changedAt}
              </li>
            ))}
          </ul>
        </div>
      )}

      {history.length === 0 && !isLoading && !error && orderId && (
        <p>이력이 없습니다.</p>
      )}

      {error && <p>{toErrorMessage(error)}</p>}
    </div>
  );
}
