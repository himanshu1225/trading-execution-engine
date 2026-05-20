package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DhanSuperOrderModifyRequest {

    private String dhanClientId;

    private String orderId;

    private String orderType;

    private String legName;

    private Integer quantity;

    private Double price;

    private Double targetPrice;

    private Double stopLossPrice;

    private Double trailingJump;
}
