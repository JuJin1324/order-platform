package com.ordersaga.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history", indexes = @Index(columnList = "orderId"))
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    protected OrderStatusHistory() {
    }

    public static OrderStatusHistory record(String orderId, OrderStatus status) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.orderId = orderId;
        history.status = status;
        history.changedAt = LocalDateTime.now();
        return history;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
