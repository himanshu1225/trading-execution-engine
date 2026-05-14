package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.decision.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.decision.TradeDecision;
import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeDecisionEngine {

    private final PricePathAnalyzer pricePathAnalyzer;
    private final RiskManagementService riskManagementService;

    @Value("${trading.threshold-percent}")
    private Double thresholdPercent;

    public TradeDecision evaluate(TradingViewAlert alert) {

        PricePathAnalysis analysis =
                pricePathAnalyzer.analyze(alert);

        Double entry = alert.getEntryPrice();
        Double stopLoss = alert.getStopLossPrice();

        // CASE 1
        if (!analysis.isEntryTouched()) {

            Integer qty =
                    riskManagementService.calculateQuantity(
                            entry,
                            stopLoss
                    );

            return TradeDecision.builder()
                    .valid(true)
                    .actionType("LIMIT")
                    .actualEntryPrice(entry)
                    .quantity(qty)
                    .decisionReason("FRESH_SETUP")
                    .build();
        }

        // CASE 2A
        if (analysis.isEntryTouched()
                && analysis.getCurrentPrice() < entry
                && analysis.getCurrentPrice() > stopLoss) {

            Integer qty =
                    riskManagementService.calculateQuantity(
                            analysis.getCurrentPrice(),
                            stopLoss
                    );

            return TradeDecision.builder()
                    .valid(true)
                    .actionType("MARKET")
                    .actualEntryPrice(analysis.getCurrentPrice())
                    .quantity(qty)
                    .decisionReason("BETTER_ENTRY")
                    .build();
        }

        // CASE 2B
        if (analysis.getCurrentPrice() <= stopLoss) {

            return TradeDecision.builder()
                    .valid(false)
                    .actionType("REJECT")
                    .decisionReason("STOPLOSS_BROKEN")
                    .build();
        }

        // CASE 2C
        if (analysis.getMaxBouncePercent() <= thresholdPercent) {

            Integer qty =
                    riskManagementService.calculateQuantity(
                            entry,
                            stopLoss
                    );

            return TradeDecision.builder()
                    .valid(true)
                    .actionType("LIMIT")
                    .actualEntryPrice(entry)
                    .quantity(qty)
                    .decisionReason("THRESHOLD_BOUNCE")
                    .build();
        }

        // CASE 2D
        return TradeDecision.builder()
                .valid(false)
                .actionType("REJECT")
                .decisionReason("STALE_MOVE")
                .build();
    }
}