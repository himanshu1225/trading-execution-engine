package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DhanForeverOrderRequest {

    private String dhanClientId;

    private String correlationId;

    private String orderFlag;

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

    private Double price1;

    private Double triggerPrice1;

    private Integer quantity1;
}
