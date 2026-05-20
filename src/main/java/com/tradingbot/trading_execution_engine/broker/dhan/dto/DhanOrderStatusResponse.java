package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanOrderStatusResponse {

    private String orderId;

    private String orderStatus;

    private String transactionType;

    private String exchangeSegment;

    private String productType;

    private String orderType;

    private String validity;

    private String tradingSymbol;

    private String securityId;

    private Integer quantity;

    private Integer filledQty;

    private Integer remainingQuantity;

    private Double price;

    private Double triggerPrice;

    private Double averageTradedPrice;

    private String createTime;

    private String updateTime;

    private String exchangeTime;

    private String drvExpiryDate;

    private String drvOptionType;

    private Double drvStrikePrice;

    private String omsErrorCode;

    private String omsErrorDescription;
}
