package com.tradingbot.trading_execution_engine.decision.service;

import com.tradingbot.trading_execution_engine.decision.model.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.alert.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.marketdata.model.Candle;
import com.tradingbot.trading_execution_engine.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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

        if (candles == null || candles.isEmpty()) {
            throw new RuntimeException(
                    "No market candles available after alert timestamp"
            );
        }

        Double entry = alert.getEntryPrice();
        Double stopLoss = alert.getStopLossPrice();

        boolean entryTouched = false;
        boolean stopLossBroken = false;
        double maxHighAfterTouch = 0.0;

        for (Candle candle : candles) {

            log.info(
                    "CANDLE => time={} open={} high={} low={} close={}",
                    candle.getTimestamp(),
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose()
            );
            // stoploss broken anytime
            if (candle.getLow() <= stopLoss) {
                stopLossBroken = true;
            }

            // entry touched for BUY
            if (!entryTouched && candle.getLow() <= entry) {
                entryTouched = true;
            }

            // bounce after entry touch
            if (entryTouched && candle.getHigh() > maxHighAfterTouch) {
                maxHighAfterTouch = candle.getHigh();
            }
        }

        double currentPrice =
                marketDataService.getLivePrice(
                        alert.getSymbol()
                );

        double maxBouncePercent = 0.0;

        if (entryTouched && maxHighAfterTouch > entry) {
            maxBouncePercent =
                    ((maxHighAfterTouch - entry) / entry) * 100;
        }

        log.info(
                "Analysis => entryTouched={}, stopLossBroken={}, currentPrice={}, maxBouncePercent={}",
                entryTouched,
                stopLossBroken,
                currentPrice,
                maxBouncePercent
        );

        return PricePathAnalysis.builder()
                .entryTouched(entryTouched)
                .stopLossBroken(stopLossBroken)
                .currentPrice(currentPrice)
                .maxBouncePercent(maxBouncePercent)
                .build();
    }
}
