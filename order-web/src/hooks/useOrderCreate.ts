import { useState } from 'react';
import { createOrder } from '../api/orderApi';
import { OrderRequest, OrderResponse } from '../types/order';

export function useOrderCreate() {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<OrderResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submitOrder(request: OrderRequest) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await createOrder(request);
      setResult(response);
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류');
    } finally {
      setIsLoading(false);
    }
  }

  return { submitOrder, isLoading, result, error };
}
