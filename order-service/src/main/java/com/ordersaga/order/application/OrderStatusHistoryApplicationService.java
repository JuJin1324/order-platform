package com.ordersaga.order.application;

import com.ordersaga.order.domain.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderStatusHistoryApplicationService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderStatusHistoryApplicationService(OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    public List<OrderStatusHistoryResult> getHistory(String orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(OrderStatusHistoryResult::from)
                .toList();
    }
}
