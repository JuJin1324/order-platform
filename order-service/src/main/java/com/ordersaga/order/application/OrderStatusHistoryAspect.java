package com.ordersaga.order.application;

import com.ordersaga.order.domain.OrderStatusHistory;
import com.ordersaga.order.domain.OrderStatusHistoryRepository;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class OrderStatusHistoryAspect {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderStatusHistoryAspect(OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    @AfterReturning(
            pointcut = "execution(* com.ordersaga.order.application.OrderApplicationService.createOrder(..)) ||" +
                       "execution(* com.ordersaga.order.application.OrderApplicationService.confirmOrder(..)) ||" +
                       "execution(* com.ordersaga.order.application.OrderApplicationService.cancelOrder(..))",
            returning = "result"
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHistory(OrderResult result) {
        orderStatusHistoryRepository.save(
                OrderStatusHistory.record(result.orderId(), result.status())
        );
    }
}
