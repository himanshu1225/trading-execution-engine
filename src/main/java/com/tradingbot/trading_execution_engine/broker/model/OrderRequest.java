package com.tradingbot.trading_execution_engine.broker.model;

import com.tradingbot.trading_execution_engine.order.model.OrderSide;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderRequest {

    private String symbol;

    private OrderSide side;

    private OrderType orderType;

    private BrokerProductType productType;

    private Integer quantity;

    private Double price;
}
