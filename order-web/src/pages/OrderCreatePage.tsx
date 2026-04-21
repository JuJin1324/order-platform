import { useOrderCreate } from '../hooks/useOrderCreate';

export default function OrderCreatePage() {
  const { submitOrder, isLoading, result, error } = useOrderCreate();

  return (
    <div>
      <h1>주문 생성</h1>
      {isLoading && <p>처리 중...</p>}
      {result && <p>주문 ID: {result.orderId}</p>}
      {error && <p>오류: {error}</p>}
    </div>
  );
}
