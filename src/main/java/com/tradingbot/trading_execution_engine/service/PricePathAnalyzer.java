package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.decision.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import org.springframework.stereotype.Service;

@Service
public class PricePathAnalyzer {

    public PricePathAnalysis analyze(TradingViewAlert alert) {

        // MOCK CASE FOR NOW

        return PricePathAnalysis.builder()
                .entryTouched(true)
                .stopLossBroken(false)
                .currentPrice(124.0)
                .maxBouncePercent(3.0)
                .build();
    }
}