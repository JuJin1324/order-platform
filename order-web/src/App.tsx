import { BrowserRouter, Routes, Route } from 'react-router-dom';
import OrderCreatePage from './pages/OrderCreatePage';
import OrderStatusPage from './pages/OrderStatusPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/orders/new" element={<OrderCreatePage />} />
        <Route path="/orders/:orderId/status" element={<OrderStatusPage />} />
      </Routes>
    </BrowserRouter>
  );
}
