package com.tradingbot.trading_execution_engine.broker.dhan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DhanPlaceOrderResponse {

    private String orderId;

    private String orderStatus;
}
