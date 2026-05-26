package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DhanMarginRequest {

    private String dhanClientId;

    private String exchangeSegment;

    private String transactionType;

    private Integer quantity;

    private String productType;

    private String securityId;

    private Double price;

    private Double triggerPrice;
}
