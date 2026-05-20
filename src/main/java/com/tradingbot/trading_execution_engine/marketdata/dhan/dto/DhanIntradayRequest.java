package com.tradingbot.trading_execution_engine.marketdata.dhan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DhanIntradayRequest {

    private String securityId;

    private String exchangeSegment;

    private String instrument;

    private String interval;

    private Boolean oi;

    private String fromDate;

    private String toDate;
}
