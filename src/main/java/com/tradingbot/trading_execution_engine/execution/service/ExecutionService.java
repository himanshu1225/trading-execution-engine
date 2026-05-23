package com.tradingbot.trading_execution_engine.execution.service;

import com.tradingbot.trading_execution_engine.broker.service.BrokerOrderService;
import com.tradingbot.trading_execution_engine.broker.model.BrokerProductType;
import com.tradingbot.trading_execution_engine.decision.model.TradeDecision;
import com.tradingbot.trading_execution_engine.persistence.entity.Order;
import com.tradingbot.trading_execution_engine.persistence.entity.Signal;
import com.tradingbot.trading_execution_engine.broker.model.OrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.PersistentOrderRequest;
import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import com.tradingbot.trading_execution_engine.order.model.OrderStatus;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final BrokerOrderService brokerOrderService;
    private final OrderRepository orderRepository;
    private final ExecutionProductResolver executionProductResolver;

    public void execute(
            Signal signal,
            TradeDecision decision) {

        OrderType orderType =
                OrderType.valueOf(decision.getActionType());

        BrokerProductType productType =
                executionProductResolver.resolveForOrder(signal);

        OrderResponse response =
                brokerOrderService.placeOrder(
                        OrderRequest.builder()
                                .symbol(signal.getSymbol())
                                .side(OrderSide.BUY)
                                .orderType(orderType)
                                .productType(productType)
                                .quantity(decision.getQuantity())
                                .price(decision.getActualEntryPrice())
                                .build()
                );

        OrderStatus orderStatus =
                OrderType.MARKET.equals(orderType)
                        ? OrderStatus.PLACED
                        : OrderStatus.PENDING;

        Order order = new Order();

        order.setBrokerOrderId(response.getBrokerOrderId());
        order.setSymbol(signal.getSymbol());
        order.setOrderPrice(decision.getActualEntryPrice());
        order.setQuantity(decision.getQuantity());
        order.setOrderType(orderType.name());
        order.setProductType(productType.name());
        order.setOrderStatus(orderStatus.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setPlacedAt(LocalDateTime.now());
        order.setSignal(signal);

        orderRepository.save(order);
    }

    public void createPersistentLimitOrder(Signal signal) {

        BrokerProductType productType =
                executionProductResolver.resolveForPersistentOrder(signal);

        OrderResponse response =
                brokerOrderService.placePersistentOrder(
                        PersistentOrderRequest.builder()
                                .symbol(signal.getSymbol())
                                .side(OrderSide.BUY)
                                .productType(productType)
                                .quantity(signal.getQuantity())
                                .triggerPrice(signal.getEntryPrice())
                                .limitPrice(signal.getEntryPrice())
                                .stopLossPrice(signal.getStopLossPrice())
                                .build()
                );

        Order order = new Order();

        order.setBrokerOrderId(response.getBrokerOrderId());
        order.setSymbol(signal.getSymbol());
        order.setOrderPrice(signal.getEntryPrice());
        order.setQuantity(signal.getQuantity());
        order.setOrderType(OrderType.PERSISTENT_LIMIT.name());
        order.setProductType(productType.name());
        order.setOrderStatus(OrderStatus.FOREVER_ACTIVE.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setPlacedAt(LocalDateTime.now());
        order.setSignal(signal);

        orderRepository.save(order);
    }
}
