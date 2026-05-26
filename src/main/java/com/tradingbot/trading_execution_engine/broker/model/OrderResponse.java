package com.tradingbot.trading_execution_engine.broker.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private String brokerOrderId;

    private String securityId;

    private String exchangeSegment;

    private BrokerOrderStatus status;

    private String message;
}
