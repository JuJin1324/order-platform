import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
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

  const [sku, setSku] = useState('sku-001');
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
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-md w-full max-w-md p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">주문 생성</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">상품 ID (SKU)</label>
            <input
              value={sku}
              onChange={(e) => setSku(e.target.value)}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">수량</label>
            <input
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">결제 금액 (원)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-medium py-2 rounded-lg transition-colors"
          >
            {isLoading ? '처리 중...' : '주문 생성'}
          </button>
        </form>
        {error && (
          <p className="mt-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
            {toErrorMessage(error)}
          </p>
        )}
      </div>
    </div>
  );
}
