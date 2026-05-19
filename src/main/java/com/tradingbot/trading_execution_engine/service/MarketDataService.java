package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.market.Candle;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketDataService {

    List<Candle> getCandlesAfterAlert(
            String symbol,
            LocalDateTime alertTimestamp
    );

    Double getLivePrice(String symbol);
}