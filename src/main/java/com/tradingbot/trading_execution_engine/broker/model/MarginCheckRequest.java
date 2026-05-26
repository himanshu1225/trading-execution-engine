package com.tradingbot.trading_execution_engine.broker.model;

import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarginCheckRequest {

    private String symbol;

    private OrderSide side;

    private BrokerProductType productType;

    private Integer quantity;

    private Double price;

    private Double triggerPrice;
}
