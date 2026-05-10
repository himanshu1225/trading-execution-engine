package com.tradingbot.trading_execution_engine.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradingViewAlert {

    private String symbol;

    private Double entryPrice;

    private Double stopLoss;

    private Double zoneHigh;

    private Double zoneLow;

    private String setupType;
}