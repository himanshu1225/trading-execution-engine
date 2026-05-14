package com.tradingbot.trading_execution_engine.integration;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BrokerClient {

    public String placeLimitOrder(
            String symbol,
            Double price,
            Integer quantity) {

        return "LIMIT-" + UUID.randomUUID();
    }

    public String placeMarketOrder(
            String symbol,
            Integer quantity) {

        return "MARKET-" + UUID.randomUUID();
    }
}