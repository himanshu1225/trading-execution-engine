package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DhanSuperOrderRequest {

    private String dhanClientId;

    private String correlationId;

    private String transactionType;

    private String exchangeSegment;

    private String productType;

    private String orderType;

    private String securityId;

    private Integer quantity;

    private Double price;

    private Double targetPrice;

    private Double stopLossPrice;

    private Double trailingJump;
}
