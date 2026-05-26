package com.tradingbot.trading_execution_engine.order.lifecycle;

import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.execution.service.ExecutionService;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import com.tradingbot.trading_execution_engine.broker.model.BrokerOrderStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.broker.model.OrderStatusResponse;
import com.tradingbot.trading_execution_engine.order.model.SignalStatus;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import com.tradingbot.trading_execution_engine.persistence.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMonitorService {

    private final OrderRepository orderRepository;
    private final SignalRepository signalRepository;
    private final BrokerOrderService brokerOrderService;
    private final ExecutionService executionService;

    @Transactional
    public void monitorPendingOrders() {

        List<Order> pendingOrders =
                orderRepository.findByOrderStatus(OrderStatus.PENDING.name());

        log.info("Found {} pending orders",
                pendingOrders.size());

        for (Order order : pendingOrders) {

            OrderStatusResponse brokerStatus =
                    brokerOrderService.getOrderStatus(
                            order.getBrokerOrderId()
                    );

            processStatus(order, brokerStatus);
        }
    }

    private void processStatus(
            Order order,
            OrderStatusResponse brokerStatus) {

        Signal signal = order.getSignal();

        BrokerOrderStatus status =
                brokerStatus.getStatus();

        switch (status) {

            case FILLED:
                order.setOrderStatus(OrderStatus.FILLED.name());
                order.setFilledAt(LocalDateTime.now());

                signal.setStatus(SignalStatus.FILLED.name());
                break;

            case CANCELLED:
                order.setOrderStatus(OrderStatus.CANCELLED.name());
                order.setCancelledAt(LocalDateTime.now());

                signal.setStatus(SignalStatus.CANCELLED.name());
                break;

            case EXPIRED:
                order.setOrderStatus(OrderStatus.EXPIRED.name());
                order.setCancelledAt(LocalDateTime.now());

                signal.setStatus(SignalStatus.PENDING_FOREVER.name());

                executionService.createPersistentLimitOrder(signal);
                signal.setStatus(SignalStatus.FOREVER_ACTIVE.name());
                break;

            default:
                log.info("Order still pending: {}",
                        order.getId());
                return;
        }

        orderRepository.save(order);
        signalRepository.save(signal);

        log.info("Processed order {} with status {}",
                order.getId(),
                status);
    }
}
