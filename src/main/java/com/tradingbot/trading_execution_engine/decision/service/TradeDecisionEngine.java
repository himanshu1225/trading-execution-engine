package com.tradingbot.trading_execution_engine.decision.service;

import com.tradingbot.trading_execution_engine.decision.model.PricePathAnalysis;
import com.tradingbot.trading_execution_engine.decision.model.TradeDecision;
import com.tradingbot.trading_execution_engine.decision.model.TradeTargetPlan;
import com.tradingbot.trading_execution_engine.alert.dto.TradingViewAlert;
import com.tradingbot.trading_execution_engine.order.model.OrderType;
import com.tradingbot.trading_execution_engine.risk.service.RiskManagementService;
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
    private final TradeTargetService tradeTargetService;

    @Value("${trading.threshold-percent}")
    private Double thresholdPercent;

    public TradeDecision evaluate(TradingViewAlert alert) {

        if (alert.getTradeScore() == null || alert.getTradeScore() < 5) {
            return TradeDecision.builder()
                    .valid(false)
                    .actionType("REJECT")
                    .decisionReason("TRADE_SCORE_BELOW_5")
                    .build();
        }

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

            TradeTargetPlan targetPlan =
                    calculateTargetPlan(
                            analysis.getCurrentPrice(),
                            stopLoss
                    );

            if (targetPlan == null) {
                return reject("INVALID_RISK_REWARD");
            }

            Integer qty =
                    riskManagementService.calculateQuantity(
                            analysis.getCurrentPrice(),
                            stopLoss
                    );

            return TradeDecision.builder()
                    .valid(true)
                    .actionType(OrderType.MARKET.name())
                    .actualEntryPrice(analysis.getCurrentPrice())
                    .quantity(qty)
                    .riskPerShare(targetPlan.getRiskPerShare())
                    .oneRPrice(targetPlan.getOneRPrice())
                    .onePointFiveRPrice(targetPlan.getOnePointFiveRPrice())
                    .twoRPrice(targetPlan.getTwoRPrice())
                    .targetPrice(targetPlan.getTargetPrice())
                    .trailingJump(targetPlan.getTrailingJump())
                    .decisionReason("BETTER_ENTRY")
                    .build();
        }

        // CASE 3 — fresh setup
        if (!analysis.isEntryTouched()) {

            TradeTargetPlan targetPlan =
                    calculateTargetPlan(
                            entry,
                            stopLoss
                    );

            if (targetPlan == null) {
                return reject("INVALID_RISK_REWARD");
            }

            Integer qty =
                    riskManagementService.calculateQuantity(
                            entry,
                            stopLoss
                    );

            return TradeDecision.builder()
                    .valid(true)
                    .actionType(OrderType.LIMIT.name())
                    .actualEntryPrice(entry)
                    .quantity(qty)
                    .riskPerShare(targetPlan.getRiskPerShare())
                    .oneRPrice(targetPlan.getOneRPrice())
                    .onePointFiveRPrice(targetPlan.getOnePointFiveRPrice())
                    .twoRPrice(targetPlan.getTwoRPrice())
                    .targetPrice(targetPlan.getTargetPrice())
                    .trailingJump(targetPlan.getTrailingJump())
                    .decisionReason("FRESH_SETUP")
                    .build();
        }

        // CASE 4 — threshold bounce
        if (analysis.getMaxBouncePercent() <= thresholdPercent) {

            TradeTargetPlan targetPlan =
                    calculateTargetPlan(
                            entry,
                            stopLoss
                    );

            if (targetPlan == null) {
                return reject("INVALID_RISK_REWARD");
            }

            Integer qty =
                    riskManagementService.calculateQuantity(
                            entry,
                            stopLoss
                    );

            return TradeDecision.builder()
                    .valid(true)
                    .actionType(OrderType.LIMIT.name())
                    .actualEntryPrice(entry)
                    .quantity(qty)
                    .riskPerShare(targetPlan.getRiskPerShare())
                    .oneRPrice(targetPlan.getOneRPrice())
                    .onePointFiveRPrice(targetPlan.getOnePointFiveRPrice())
                    .twoRPrice(targetPlan.getTwoRPrice())
                    .targetPrice(targetPlan.getTargetPrice())
                    .trailingJump(targetPlan.getTrailingJump())
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

    private TradeDecision reject(String reason) {
        return TradeDecision.builder()
                .valid(false)
                .actionType("REJECT")
                .decisionReason(reason)
                .build();
    }

    private TradeTargetPlan calculateTargetPlan(
            Double entryPrice,
            Double stopLossPrice) {

        try {
            return tradeTargetService.calculate(
                    entryPrice,
                    stopLossPrice
            );

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Unable to calculate trade target plan for entry={}, stopLoss={}, reason={}",
                    entryPrice,
                    stopLossPrice,
                    e.getMessage()
            );
            return null;
        }
    }
}
