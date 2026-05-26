package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanPositionResponse {

    private String dhanClientId;

    private String tradingSymbol;

    private String securityId;

    private String positionType;

    private String exchangeSegment;

    private String productType;

    private Double buyAvg;

    private Integer buyQty;

    private Double costPrice;

    private Double sellAvg;

    private Integer sellQty;

    private Integer netQty;

    private Double realizedProfit;

    private Double unrealizedProfit;
}
