package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.market.Candle;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketDataService {

    public List<Candle> getCandlesAfterAlert(
            String symbol,
            LocalDateTime alertTimestamp) {

        // MOCK DATA FOR NOW

        return List.of(
                new Candle(
                        alertTimestamp.plusMinutes(1),
                        125.0,
                        126.0,
                        121.0,
                        122.0
                ),
                new Candle(
                        alertTimestamp.plusMinutes(2),
                        122.0,
                        123.0,
                        119.0,
                        121.0
                ),
                new Candle(
                        alertTimestamp.plusMinutes(3),
                        121.0,
                        121.5,
                        120.0,
                        121.0
                )
        );
    }
}