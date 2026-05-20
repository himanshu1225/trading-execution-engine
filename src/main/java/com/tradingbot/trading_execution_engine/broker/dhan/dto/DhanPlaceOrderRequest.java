package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DhanPlaceOrderRequest {

    private String dhanClientId;

    private String correlationId;

    private String transactionType;

    private String exchangeSegment;

    private String productType;

    private String orderType;

    private String validity;

    private String securityId;

    private Integer quantity;

    private Integer disclosedQuantity;

    private Double price;

    private Double triggerPrice;

    private Boolean afterMarketOrder;

    private String amoTime;

    private Double boProfitValue;

    private Double boStopLossValue;
}
