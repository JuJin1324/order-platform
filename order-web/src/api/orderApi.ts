import type { OrderRequest, OrderResponse } from '../types/order';

export async function createOrder(request: OrderRequest): Promise<OrderResponse> {
  const res = await fetch('/api/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!res.ok) throw new Error(`${res.status}`);
  return res.json();
}

export async function getOrder(orderId: string): Promise<OrderResponse> {
  const res = await fetch(`/api/orders/${orderId}`);
  if (!res.ok) throw new Error(`${res.status}`);
  return res.json();
}
