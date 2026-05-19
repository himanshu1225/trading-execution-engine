package com.tradingbot.trading_execution_engine.repository;

import com.tradingbot.trading_execution_engine.entity.Signal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SignalRepository extends JpaRepository<Signal, Long> {
    boolean existsBySymbolAndEntryPriceAndStopLossPriceAndTradeTypeAndAlertDateTimeStamp(
            String symbol,
            Double entryPrice,
            Double stopLossPrice,
            String tradeType,
            String alertDateTimeStamp
    );
}
