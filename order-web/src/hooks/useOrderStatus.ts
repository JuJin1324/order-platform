import { useState } from 'react';
import { getOrder } from '../api/orderApi';
import { OrderResponse } from '../types/order';

export function useOrderStatus() {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<OrderResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function fetchOrder(orderId: string) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await getOrder(orderId);
      setResult(response);
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류');
    } finally {
      setIsLoading(false);
    }
  }

  return { fetchOrder, isLoading, result, error };
}
