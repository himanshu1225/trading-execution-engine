package com.tradingbot.trading_execution_engine.broker.model;

import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PersistentOrderRequest {

    private String symbol;

    private OrderSide side;

    private Integer quantity;

    private Double triggerPrice;

    private Double limitPrice;

    private Double stopLossPrice;
}
