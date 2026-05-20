package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanForeverOrderStatusResponse {

    private String dhanClientId;

    private String orderId;

    private String correlationId;

    private String orderStatus;

    private String orderFlag;

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

    private Double price1;

    private Double triggerPrice1;

    private Integer quantity1;
}
