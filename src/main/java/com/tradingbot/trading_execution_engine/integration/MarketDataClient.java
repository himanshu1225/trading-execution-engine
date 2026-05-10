package com.tradingbot.trading_execution_engine.integration;

import org.springframework.stereotype.Component;

@Component
public class MarketDataClient {

    public Double getCurrentPrice(String symbol) {

        // MOCK DATA FOR NOW

        if ("RELIANCE".equalsIgnoreCase(symbol)) {
            return 230.0;
        }

        return 0.0;
    }
}