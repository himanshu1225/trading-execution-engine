package com.tradingbot.trading_execution_engine.alert.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradingViewAlert {

    private String symbol;

    private String symbolDesc;

    private Double alertPrice;

    private Double entryPrice;

    private Double stopLossPrice;

    private Double locZoneHigh;

    private Double locZoneLow;

    private String tradeType;

    private Integer tradeScore;

    private String sector;

    private String alertDateTimeStamp;
}