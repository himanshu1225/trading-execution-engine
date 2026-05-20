package com.tradingbot.trading_execution_engine.order.model;

public enum OrderStatus {
    CREATED,
    PENDING,
    PLACED,
    FILLED,
    PARTIAL_FILLED,
    CANCELLED,
    REJECTED,
    FAILED,
    EXPIRED,
    CONVERTED_TO_FOREVER,
    FOREVER_ACTIVE,
    FOREVER_FILLED
}
