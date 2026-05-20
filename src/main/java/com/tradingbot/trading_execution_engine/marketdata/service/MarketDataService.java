package com.tradingbot.trading_execution_engine.marketdata.service;

import com.tradingbot.trading_execution_engine.marketdata.model.Candle;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketDataService {

    List<Candle> getCandlesAfterAlert(
            String symbol,
            LocalDateTime alertTimestamp
    );

    Double getLivePrice(String symbol);
}