package com.tradingbot.trading_execution_engine.decision.service;

import com.tradingbot.trading_execution_engine.decision.model.TradeTargetPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TradeTargetService {

    @Value("${trading.trailing-jump-percent:0.5}")
    private Double trailingJumpPercent;

    public TradeTargetPlan calculate(
            Double entryPrice,
            Double stopLossPrice) {

        if (entryPrice == null || stopLossPrice == null) {
            throw new IllegalArgumentException(
                    "Entry price and stop loss price are required"
            );
        }

        double riskPerShare =
                entryPrice - stopLossPrice;

        if (riskPerShare <= 0) {
            throw new IllegalArgumentException(
                    "Stop loss must be below entry price for BUY trades"
            );
        }

        double oneRPrice =
                entryPrice + riskPerShare;

        double onePointFiveRPrice =
                entryPrice + (riskPerShare * 1.5);

        double twoRPrice =
                entryPrice + (riskPerShare * 2);

        double trailingJump =
                entryPrice * (trailingJumpPercent / 100);

        return TradeTargetPlan.builder()
                .riskPerShare(riskPerShare)
                .oneRPrice(oneRPrice)
                .onePointFiveRPrice(onePointFiveRPrice)
                .twoRPrice(twoRPrice)
                .targetPrice(twoRPrice)
                .trailingJump(trailingJump)
                .build();
    }
}
