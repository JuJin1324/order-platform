export type OrderStatus = 'CREATED' | 'CONFIRMED' | 'CANCELLED';

export interface OrderRequest {
  sku: string;
  quantity: number;
  amount: number;
}

export interface OrderResponse {
  orderId: string;
  status: OrderStatus;
  sku: string;
  quantity: number;
  amount: number;
}
