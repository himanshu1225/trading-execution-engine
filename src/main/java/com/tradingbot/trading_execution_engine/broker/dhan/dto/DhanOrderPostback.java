package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanOrderPostback {

    private String dhanClientId;

    private String orderId;

    private String correlationId;

    private String orderStatus;

    private String transactionType;

    private String exchangeSegment;

    private String productType;

    private String orderType;

    private String validity;

    private String tradingSymbol;

    private String securityId;

    private Integer quantity;

    private Integer disclosedQuantity;

    private Double price;

    private Double triggerPrice;

    private Boolean afterMarketOrder;

    private Double boProfitValue;

    private Double boStopLossValue;

    private String legName;

    private String createTime;

    private String updateTime;

    private String exchangeTime;

    private String drvExpiryDate;

    private String drvOptionType;

    private Double drvStrikePrice;

    private String omsErrorCode;

    private String omsErrorDescription;

    @JsonAlias("filled_qty")
    private Integer filledQty;

    private String algoId;
}
