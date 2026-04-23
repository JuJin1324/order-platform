import { useState } from 'react';
import { getOrderStatusHistory } from '../api/orderApi';
import type { OrderStatusHistory } from '../types/order';

export function useOrderStatus() {
  const [isLoading, setIsLoading] = useState(false);
  const [history, setHistory] = useState<OrderStatusHistory[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [lastOrderId, setLastOrderId] = useState<string | null>(null);

  async function fetchHistory(orderId: string) {
    setIsLoading(true);
    setError(null);
    setLastOrderId(orderId);
    try {
      const result = await getOrderStatusHistory(orderId);
      setHistory(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : '알 수 없는 오류');
    } finally {
      setIsLoading(false);
    }
  }

  async function refresh() {
    if (lastOrderId) await fetchHistory(lastOrderId);
  }

  return { fetchHistory, refresh, isLoading, history, error };
}
