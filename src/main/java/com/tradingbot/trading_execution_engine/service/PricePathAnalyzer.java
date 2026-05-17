package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.decision.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.market.Candle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricePathAnalyzer {

    private final MarketDataService marketDataService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public PricePathAnalysis analyze(TradingViewAlert alert) {

        LocalDateTime alertTimestamp =
                LocalDateTime.parse(
                        alert.getAlertDateTimeStamp(),
                        FORMATTER
                );

        List<Candle> candles =
                marketDataService.getCandlesAfterAlert(
                        alert.getSymbol(),
                        alertTimestamp
                );

        Double entry = alert.getEntryPrice();
        Double stopLoss = alert.getStopLossPrice();

        boolean entryTouched = false;
        boolean stopLossBroken = false;
        double maxHighAfterTouch = 0.0;
        double currentPrice = 0.0;

        for (Candle candle : candles) {

            currentPrice = candle.getClose();

            if (!entryTouched && candle.getLow() <= entry) {
                entryTouched = true;
            }

            if (candle.getLow() <= stopLoss) {
                stopLossBroken = true;
            }

            if (entryTouched && candle.getHigh() > maxHighAfterTouch) {
                maxHighAfterTouch = candle.getHigh();
            }
        }

        double maxBouncePercent = 0.0;

        if (entryTouched && maxHighAfterTouch > entry) {
            maxBouncePercent =
                    ((maxHighAfterTouch - entry) / entry) * 100;
        }

        return PricePathAnalysis.builder()
                .entryTouched(entryTouched)
                .stopLossBroken(stopLossBroken)
                .currentPrice(currentPrice)
                .maxBouncePercent(maxBouncePercent)
                .build();
    }
}