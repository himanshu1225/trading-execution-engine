package com.tradingbot.trading_execution_engine.service;

import com.tradingbot.trading_execution_engine.decision.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.decision.TradeDecision;
import com.tradingbot.trading_execution_engine.dto.TradingViewAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
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

        log.info("========== TRADE DECISION DEBUG ==========");
        log.info("Symbol={}", alert.getSymbol());
        log.info("Entry={}", entry);
        log.info("StopLoss={}", stopLoss);
        log.info("ThresholdPercent={}", thresholdPercent);
        log.info("EntryTouched={}", analysis.isEntryTouched());
        log.info("StopLossBroken={}", analysis.isStopLossBroken());
        log.info("CurrentPrice={}", analysis.getCurrentPrice());
        log.info("MaxBouncePercent={}", analysis.getMaxBouncePercent());
        log.info("==========================================");

        // CASE 1 — setup failed
        if (analysis.isStopLossBroken()) {
            return TradeDecision.builder()
                    .valid(false)
                    .actionType("REJECT")
                    .decisionReason("STOPLOSS_BROKEN")
                    .build();
        }

        // CASE 2 — better price available
        if (analysis.getCurrentPrice() <= entry) {

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

        // CASE 3 — fresh setup
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

        // CASE 4 — threshold bounce
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

        // CASE 5 — stale move
        return TradeDecision.builder()
                .valid(false)
                .actionType("REJECT")
                .decisionReason("STALE_MOVE")
                .build();
    }
}