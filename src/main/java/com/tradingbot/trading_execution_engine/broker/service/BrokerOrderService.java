package com.tradingbot.trading_execution_engine.broker.service;

import com.tradingbot.trading_execution_engine.broker.model.OrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.OrderResponse;
import com.tradingbot.trading_execution_engine.broker.model.OrderStatusResponse;
import com.tradingbot.trading_execution_engine.broker.model.PersistentOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderRequest;
import com.tradingbot.trading_execution_engine.broker.model.SuperOrderLeg;

public interface BrokerOrderService {

    OrderResponse placeOrder(OrderRequest request);

    OrderResponse placePersistentOrder(PersistentOrderRequest request);

    OrderResponse placeSuperOrder(SuperOrderRequest request);

    OrderStatusResponse getOrderStatus(String brokerOrderId);

    void cancelOrder(String brokerOrderId);

    void cancelSuperOrderLeg(
            String brokerOrderId,
            SuperOrderLeg leg);
}
