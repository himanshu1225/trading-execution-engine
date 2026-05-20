package com.tradingbot.trading_execution_engine.marketdata.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Candle {

    private LocalDateTime timestamp;

    private Double open;

    private Double high;

    private Double low;

    private Double close;
}