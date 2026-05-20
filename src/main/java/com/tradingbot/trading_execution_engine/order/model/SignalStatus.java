package com.tradingbot.trading_execution_engine.order.model;

public enum SignalStatus {
    REJECTED,
    MARKET_EXECUTED,
    PENDING_LIMIT,
    FILLED,
    CANCELLED,
    PENDING_FOREVER,
    CONVERTED_TO_FOREVER,
    FOREVER_ACTIVE,
    FOREVER_FILLED
}
