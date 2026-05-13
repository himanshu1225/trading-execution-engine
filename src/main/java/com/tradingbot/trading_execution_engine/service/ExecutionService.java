package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.entity.Order;
import com.tradingbot.trading_execution_engine.entity.Signal;
import com.tradingbot.trading_execution_engine.integration.BrokerClient;
import com.tradingbot.trading_execution_engine.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final BrokerClient brokerClient;
    private final OrderRepository orderRepository;

    public void execute(Signal signal) {

        String brokerOrderId =
                brokerClient.placeLimitOrder(
                        signal.getSymbol(),
                        signal.getEntryPrice(),
                        10
                );

        Order order = new Order();

        order.setBrokerOrderId(brokerOrderId);
        order.setSymbol(signal.getSymbol());
        order.setOrderPrice(signal.getEntryPrice());
        order.setQuantity(10);
        order.setOrderType("LIMIT");
        order.setOrderStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());
        order.setSignal(signal);

        orderRepository.save(order);
    }
}