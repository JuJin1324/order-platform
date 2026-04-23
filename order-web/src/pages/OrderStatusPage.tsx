import { useState, FormEvent, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useOrderStatus } from '../hooks/useOrderStatus';

function toErrorMessage(_error: string): string {
  return '네트워크 오류가 발생했습니다. 연결 상태를 확인해주세요.';
}

const STATUS_STYLE: Record<string, string> = {
  CREATED:   'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
};

export default function OrderStatusPage() {
  const { orderId: urlOrderId } = useParams<{ orderId: string }>();
  const { fetchHistory, refresh, isLoading, history, error } = useOrderStatus();
  const [orderId, setOrderId] = useState(urlOrderId ?? '');

  useEffect(() => {
    if (urlOrderId) fetchHistory(urlOrderId);
  }, [urlOrderId]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    fetchHistory(orderId);
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-md w-full max-w-md p-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">주문 상태 조회</h1>
        <form onSubmit={handleSubmit} className="flex gap-2 mb-6">
          <input
            value={orderId}
            onChange={(e) => setOrderId(e.target.value)}
            required
            placeholder="주문 ID"
            className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="submit"
            disabled={isLoading}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            조회
          </button>
        </form>

        {history.length > 0 && (
          <div>
            <div className="flex justify-between items-center mb-3">
              <span className="text-sm font-medium text-gray-600">상태 이력</span>
              <button
                onClick={refresh}
                disabled={isLoading}
                className="text-sm text-blue-600 hover:text-blue-800 disabled:text-gray-400"
              >
                새로고침
              </button>
            </div>
            <ul className="space-y-2">
              {history.map((item, index) => (
                <li key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <span className={`text-xs font-semibold px-2 py-1 rounded-full ${STATUS_STYLE[item.status] ?? 'bg-gray-100 text-gray-700'}`}>
                    {item.status}
                  </span>
                  <span className="text-xs text-gray-500">{item.changedAt}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {history.length === 0 && !isLoading && !error && orderId && (
          <p className="text-sm text-gray-500 text-center py-4">이력이 없습니다.</p>
        )}

        {error && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
            {toErrorMessage(error)}
          </p>
        )}
      </div>
    </div>
  );
}
