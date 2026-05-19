package com.tradingbot.trading_execution_engine.market;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstrumentInfo {

    private String securityId;

    private String exchangeSegment;

    private String instrument;
}