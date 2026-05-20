package com.tradingbot.trading_execution_engine.broker.model;

public enum BrokerOrderStatus {
    PENDING,
    PLACED,
    FILLED,
    PARTIAL_FILLED,
    CANCELLED,
    REJECTED,
    EXPIRED,
    FAILED
}
