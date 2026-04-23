package com.ordersaga.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(String orderId);
}
