package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.entity.Order;
import com.tradingbot.trading_execution_engine.entity.Signal;
import com.tradingbot.trading_execution_engine.integration.BrokerClient;
import com.tradingbot.trading_execution_engine.repository.OrderRepository;
import com.tradingbot.trading_execution_engine.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMonitorService {

    private final OrderRepository orderRepository;
    private final SignalRepository signalRepository;
    private final BrokerClient brokerClient;
    private final ExecutionService executionService;

    public void monitorPendingOrders() {

        List<Order> pendingOrders =
                orderRepository.findByOrderStatus("PENDING");

        log.info("Found {} pending orders",
                pendingOrders.size());

        for (Order order : pendingOrders) {

            String brokerStatus =
                    brokerClient.getOrderStatus(
                            order.getBrokerOrderId()
                    );

            processStatus(order, brokerStatus);
        }
    }

    private void processStatus(
            Order order,
            String brokerStatus) {

        Signal signal = order.getSignal();

        switch (brokerStatus) {

            case "FILLED":
                order.setOrderStatus("FILLED");
                order.setFilledAt(LocalDateTime.now());

                signal.setStatus("FILLED");
                break;

            case "CANCELLED":
                order.setOrderStatus("CANCELLED");
                order.setCancelledAt(LocalDateTime.now());

                signal.setStatus("CANCELLED");
                break;

            case "EXPIRED":
                order.setOrderStatus("EXPIRED");
                order.setCancelledAt(LocalDateTime.now());

                signal.setStatus("PENDING_FOREVER");

                executionService.recreateLimitOrder(signal);
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
                brokerStatus);
    }
}