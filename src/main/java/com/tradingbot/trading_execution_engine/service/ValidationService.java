package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.integration.MarketDataClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final MarketDataClient marketDataClient;

    public boolean validateEntry(TradingViewAlert alert) {

        Double currentPrice =
                marketDataClient.getCurrentPrice(alert.getSymbol());

        Double allowedPrice =
                alert.getEntryPrice() * 1.01;

        return currentPrice <= allowedPrice;
    }
}