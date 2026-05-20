package com.tradingbot.trading_execution_engine.broker.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusResponse {

    private String brokerOrderId;

    private BrokerOrderStatus status;

    private Integer filledQuantity;

    private Double averagePrice;

    private String message;
}
