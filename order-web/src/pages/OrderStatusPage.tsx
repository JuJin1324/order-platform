import { useOrderStatus } from '../hooks/useOrderStatus';

export default function OrderStatusPage() {
  const { fetchOrder, isLoading, result, error } = useOrderStatus();

  return (
    <div>
      <h1>주문 상태</h1>
      {isLoading && <p>조회 중...</p>}
      {result && <p>상태: {result.status}</p>}
      {error && <p>오류: {error}</p>}
    </div>
  );
}
