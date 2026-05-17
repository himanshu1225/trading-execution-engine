package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.decision.TradeDecision;
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

    public void execute(
            Signal signal,
            TradeDecision decision) {

        String brokerOrderId;

        if ("MARKET".equals(decision.getActionType())) {

            brokerOrderId =
                    brokerClient.placeMarketOrder(
                            signal.getSymbol(),
                            decision.getQuantity()
                    );

        } else {

            brokerOrderId =
                    brokerClient.placeLimitOrder(
                            signal.getSymbol(),
                            decision.getActualEntryPrice(),
                            decision.getQuantity()
                    );
        }

        Order order = new Order();

        order.setBrokerOrderId(brokerOrderId);
        order.setSymbol(signal.getSymbol());
        order.setOrderPrice(decision.getActualEntryPrice());
        order.setQuantity(decision.getQuantity());
        order.setOrderType(decision.getActionType());
        if ("MARKET".equals(decision.getActionType())) {
            order.setOrderStatus("PLACED");
        } else {
            order.setOrderStatus("PENDING");
        }
        order.setCreatedAt(LocalDateTime.now());
        order.setPlacedAt(LocalDateTime.now());
        order.setSignal(signal);

        orderRepository.save(order);
    }

    public void recreateLimitOrder(Signal signal) {

        String brokerOrderId =
                brokerClient.placeLimitOrder(
                        signal.getSymbol(),
                        signal.getEntryPrice(),
                        signal.getQuantity()
                );

        Order order = new Order();

        order.setBrokerOrderId(brokerOrderId);
        order.setSymbol(signal.getSymbol());
        order.setOrderPrice(signal.getEntryPrice());
        order.setQuantity(signal.getQuantity());
        order.setOrderType("LIMIT");
        order.setOrderStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setPlacedAt(LocalDateTime.now());
        order.setSignal(signal);

        orderRepository.save(order);
    }
}